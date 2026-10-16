package com.example.catalogo.sync.application.usecases;

import java.time.Clock;

import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import com.example.catalogo.professional.domain.ports.out.ProfessionalRepository;
import com.example.catalogo.professional.domain.ports.out.WeeklyScheduleRepository;
import com.example.catalogo.sync.application.exception.CatalogSnapshotException;
import com.example.catalogo.sync.domain.model.CatalogSnapshot;
import com.example.catalogo.sync.domain.model.SyncType;
import com.example.catalogo.sync.domain.ports.in.SynchronizeCatalogUseCase;
import com.example.catalogo.sync.domain.ports.out.CatalogSnapshotPort;
import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

@Slf4j
@Component
@RequiredArgsConstructor
public class SynchronizeCatalogUseCaseImpl implements SynchronizeCatalogUseCase {

	private final SyncStateRepository syncStateRepository;
	private final CatalogSnapshotPort catalogSnapshotPort;
	private final CategoryRepository categoryRepository;
	private final ProfessionalRepository professionalRepository;
	private final WeeklyScheduleRepository weeklyScheduleRepository;
	private final TransactionOperations transactionOperations;
	private final Clock clock;

	// Por ahora la única situación de la tabla de docs/arq/sincronizacion.md es la base vacía: sin
	// versión local, snapshot. Las comparaciones con Redis (incremental, discontinuidad) son de la M5.
	@Override
	public void synchronizeCatalog() {
		Long localVersion = syncStateRepository.getCurrent().getLocalVersion();
		if (localVersion == null) {
			rebuildFromSnapshot(null);
		}
	}

	// CU-02. Reemplazar la copia entera (borrar y volver a guardar) es simple y obviamente correcto,
	// y su costo no importa: el snapshot solo se usa al iniciar con la base vacía o para recuperarse.
	// Lo habitual es la vía incremental, que guarda por id solo lo que cambió.
	private void rebuildFromSnapshot(Long expectedVersion) {
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
			} else {
				// Otro ciclo cambió la versión mientras tanto (CU-02, 3a): no es un error.
				syncStateRepository.clearSyncInProgress();
				log.info("Catalog snapshot discarded: another sync already changed the local version");
			}
		} catch (CatalogSnapshotException e) {
			// El error se registra fuera de la transacción, que ya se deshizo: si no, se desharía con
			// ella (ADR-0011). No se reintenta ahora: lo retoma el próximo disparador.
			log.warn("Catalog snapshot failed: {}", e.getMessage());
			syncStateRepository.recordFailure(e.getMessage(), clock.instant());
		} catch (RuntimeException e) {
			// Una falla propia (por ejemplo, la base rechaza una referencia inconsistente). El detalle
			// queda solo en el log; el estado expuesto dice que es interna y de qué tipo.
			log.error("Catalog snapshot could not be stored", e);
			syncStateRepository.recordFailure(
					"Internal error: catalog snapshot could not be stored (" + e.getClass().getSimpleName() + ")",
					clock.instant());
		}
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
