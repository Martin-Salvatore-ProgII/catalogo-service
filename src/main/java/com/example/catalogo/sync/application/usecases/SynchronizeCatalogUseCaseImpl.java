package com.example.catalogo.sync.application.usecases;

import java.time.Clock;

import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import com.example.catalogo.professional.domain.ports.out.ProfessionalRepository;
import com.example.catalogo.professional.domain.ports.out.WeeklyScheduleRepository;
import com.example.catalogo.sync.application.exception.CatalogChangesException;
import com.example.catalogo.sync.application.exception.CatalogSnapshotException;
import com.example.catalogo.sync.domain.model.CatalogSnapshot;
import com.example.catalogo.sync.domain.model.CatalogVersions;
import com.example.catalogo.sync.domain.model.SyncType;
import com.example.catalogo.sync.domain.ports.in.SynchronizeCatalogUseCase;
import com.example.catalogo.sync.domain.ports.out.CatalogSnapshotPort;
import com.example.catalogo.sync.domain.ports.out.CatalogVersionPort;
import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Un ciclo de sincronización (CU-01, CU-02): compara la versión local (L) con la actual (C) y la más vieja disponible (O) 
 * que publica la cátedra en Redis en Redis, y decide según la tabla de situaciones de {@code docs/arq/sincronizacion.md}.
 * Nunca lanza una excepción: una falla se registra en el estado de sincronización y la retoma el próximo disparador (ADR-0011).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SynchronizeCatalogUseCaseImpl implements SynchronizeCatalogUseCase {

	private final SyncStateRepository syncStateRepository;
	private final CatalogSnapshotPort catalogSnapshotPort;
	private final CatalogVersionPort catalogVersionPort;
	private final IncrementalVersionApplier incrementalVersionApplier;
	private final CategoryRepository categoryRepository;
	private final ProfessionalRepository professionalRepository;
	private final WeeklyScheduleRepository weeklyScheduleRepository;
	private final TransactionOperations transactionOperations;
	private final Clock clock;

	@Override
	public void synchronizeCatalog() {
		try {
			runCycle();
		} catch (CatalogChangesException e) {
			// Redis no respondió o devolvió algo sin sentido. El mensaje es seguro de mostrar.
			log.warn("Catalog sync failed: {}", e.getMessage());
			syncStateRepository.recordFailure(e.getMessage(), clock.instant());
		} catch (RuntimeException e) {
			// Una falla propia (por ejemplo, la base rechaza una referencia inconsistente). El
			// detalle queda solo en el log; el estado expuesto dice que es interna y de qué tipo.
			log.error("Catalog sync could not be completed", e);
			syncStateRepository.recordFailure(
					"Internal error: catalog could not be synchronized (" + e.getClass().getSimpleName() + ")",
					clock.instant());
		}
	}

	// La tabla de situaciones. Después de un snapshot se vuelve a comparar con Redis: mientras se
	// aplicaba pudo publicarse una versión más nueva (CU-02, paso 4).
	private void runCycle() {
		Long local = syncStateRepository.getCurrent().getLocalVersion();
		// Un solo snapshot por ciclo: si ni después de reemplazar la copia el historial cierra, otro
		// snapshot no lo arreglaría, y repetirlo sin fin es lo que ENUNCIADO 8 prohíbe. Se
		// registra el error y lo retoma el próximo disparador.
		boolean snapshotTaken = false;
		while (true) {
			if (local == null) {
				// Base vacía: no hay con qué comparar, y el snapshot trae su propia versión.
				local = rebuildFromSnapshot(null);
				if (local == null) {
					return;
				}
				snapshotTaken = true;
			}
			CatalogVersions versions = catalogVersionPort.getVersions();
			if (local == versions.getCurrent()) {
				return;
			}
			if (snapshotTaken && local > versions.getCurrent()) {
				// El snapshot es más nuevo que lo que Redis informa todavía: se está publicando.
				// Otro snapshot no ayuda; el próximo chequeo compara de nuevo.
				log.info("Catalog snapshot {} is ahead of Redis version {}", local, versions.getCurrent());
				return;
			}
			boolean historyIsContinuous = local >= versions.getOldestAvailable() && local < versions.getCurrent();
			if (historyIsContinuous) {
				Long reached = applyVersions(local, versions.getCurrent());
				if (reached == null || reached == versions.getCurrent()) {
					return;
				}
				// Una versión no se pudo verificar: hasta `reached` quedó aplicado y de ahí en
				// adelante corresponde un snapshot.
				local = reached;
			}
			if (snapshotTaken) {
				throw new CatalogChangesException(
						"The catalog history could not be verified even after a snapshot; it will be retried");
			}
			local = rebuildFromSnapshot(local);
			if (local == null) {
				return;
			}
			snapshotTaken = true;
		}
	}

	// CU-01, paso 4: aplicar las versiones en orden, de a una. Devuelve la última versión aplicada, o null
	// si otro ciclo ya cambió la local y este debe terminar sin error (ADR-0007). Si una versión no
	// se puede verificar, devuelve hasta dónde llegó, y esa versión queda sin aplicar.
	private Long applyVersions(long local, long current) {
		long reached = local;
		for (long version = local + 1; version <= current; version++) {
			// Aplicar una versión apaga la marca de "en curso"; si faltan más, se vuelve a encender.
			syncStateRepository.markSyncStarted();
			IncrementalVersionResult result = incrementalVersionApplier.apply(version);
			if (result == IncrementalVersionResult.CONCURRENT_CHANGE) { //otro ciclo cambio la version
				syncStateRepository.clearSyncInProgress();
				log.info("Catalog version {} discarded: another sync already changed the local version", version);
				return null;
			}
			if (result == IncrementalVersionResult.UNVERIFIABLE) { // corta y deja ahi
				return reached;
			}
			reached = version;  // todo OK y sigue con la proxima
			log.info("Catalog advanced to version {}", version);
		}
		return reached;
	}

	// CU-02. Reemplazar la copia entera (borrar y volver a guardar)
	// el snapshot solo se usa al iniciar con la base vacía o para recuperarse.
	// Lo habitual es la vía incremental, que guarda por id solo lo que cambió.
	// Devuelve la versión del snapshot aplicado, o null si no se aplicó: el ciclo termina.
	private Long rebuildFromSnapshot(Long expectedVersion) {
		syncStateRepository.markSyncStarted();
		try {
			// El pedido a la cátedra va antes de la transacción: la base no queda comprometida
			// mientras se espera la respuesta (ADR-0007).
			CatalogSnapshot snapshot = catalogSnapshotPort.getSnapshot();
			boolean applied = Boolean.TRUE.equals(transactionOperations.execute(status -> {
				boolean versionSaved = replaceCatalog(snapshot, expectedVersion);
				if (!versionSaved) {
					status.setRollbackOnly();
				}
				return versionSaved;
			}));
			if (applied) {
				log.info("Catalog rebuilt from snapshot version {}", snapshot.getSnapshotVersion());
				return snapshot.getSnapshotVersion();
			}
			// Otro ciclo cambió la versión mientras tanto (CU-02, 3a): no es un error.
			syncStateRepository.clearSyncInProgress();
			log.info("Catalog snapshot discarded: another sync already changed the local version");
		} catch (CatalogSnapshotException e) {
			// El error se registra fuera de la transacción, que ya se deshizo: si no, se desharía con
			// ella (ADR-0011). No se reintenta ahora: lo retoma el próximo disparador.
			log.warn("Catalog snapshot failed: {}", e.getMessage());
			syncStateRepository.recordFailure(e.getMessage(), clock.instant());
		} catch (RuntimeException e) {
			log.error("Catalog snapshot could not be stored", e);
			syncStateRepository.recordFailure(
					"Internal error: catalog snapshot could not be stored (" + e.getClass().getSimpleName() + ")",
					clock.instant());
		}
		return null;
	}

	// Todo en la misma transacción (ADR-0010): las búsquedas ven la copia anterior completa hasta
	// el final. Se borra de hijos a padres y se guarda de padres a hijos, por las claves foráneas.
	private boolean replaceCatalog(CatalogSnapshot snapshot, Long expectedVersion) {
		weeklyScheduleRepository.deleteAll();
		professionalRepository.deleteAll();
		categoryRepository.deleteAll();
		categoryRepository.saveAll(snapshot.getCategories());
		professionalRepository.saveAll(snapshot.getProfessionals());
		weeklyScheduleRepository.saveAll(snapshot.getWeeklySchedules());
		return syncStateRepository.recordSuccessIfVersionIs(
				expectedVersion, snapshot.getSnapshotVersion(), SyncType.SNAPSHOT, clock.instant());
	}

}
