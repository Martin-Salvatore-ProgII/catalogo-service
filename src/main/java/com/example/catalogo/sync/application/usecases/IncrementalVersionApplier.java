package com.example.catalogo.sync.application.usecases;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.professional.domain.ports.out.ProfessionalRepository;
import com.example.catalogo.professional.domain.ports.out.WeeklyScheduleRepository;
import com.example.catalogo.sync.domain.model.SyncType;
import com.example.catalogo.sync.domain.model.VersionChanges;
import com.example.catalogo.sync.domain.ports.out.CatalogChangesPort;
import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Aplica una versión del catálogo por la vía incremental (CU-01, paso 4): lee de Redis lo que
 * cambió, guarda los cambios y la versión local en una sola transacción (ADR-0007, ADR-0010).
 *
 * <p>No es un caso de uso: no tiene puerto de entrada. Es una pieza de {@link
 * SynchronizeCatalogUseCaseImpl}, separada para que el caso de uso que decide el ciclo (la tabla
 * de situaciones de {@code sincronizacion.md}) no cargue también con el detalle de aplicar una
 * versión. Depende solo de puertos de salida.
 *
 * <p>Si Redis falla, deja pasar la {@code CatalogChangesException} del puerto: registrar el error
 * es del ciclo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class IncrementalVersionApplier {

	private final CatalogChangesPort catalogChangesPort;
	private final SyncStateRepository syncStateRepository;
	private final CategoryRepository categoryRepository;
	private final ProfessionalRepository professionalRepository;
	private final WeeklyScheduleRepository weeklyScheduleRepository;
	private final TransactionOperations transactionOperations;  // usamos el de Spring
	private final Clock clock;

	/** Aplica la versión {@code version}, que tiene que seguir a la local {@code version - 1}. */
	IncrementalVersionResult apply(long version) {
	// Antes de la transacción se chequea que no haya quedado nulo o inverificable lo que vamos a operar (ADR-0007).
		VersionChanges changes = catalogChangesPort.findChanges(version).orElse(null);
		if (changes == null) {
			log.warn("Catalog version {} has no readable changes", version);
			return IncrementalVersionResult.UNVERIFIABLE;
		}
		VersionContent content = readContent(version, changes);
		if (content == null) {
			return IncrementalVersionResult.UNVERIFIABLE;
		}
		boolean applied = Boolean.TRUE.equals(transactionOperations.execute(status -> {
			// guardamos padres antes que hijos, por las claves foráneas.
			categoryRepository.saveAll(content.categories());
			professionalRepository.saveAll(content.professionals());
			weeklyScheduleRepository.saveAll(content.schedules());
			// La versión avanza solo si la local sigue siendo la anterior, en la misma transacción que
			// los datos (ADR-0007). Si otro ciclo la cambió, se deshace todo.
			boolean versionSaved = syncStateRepository.recordSuccessIfVersionIs(
					version - 1, version, SyncType.INCREMENTAL, clock.instant());
			if (!versionSaved) {
				status.setRollbackOnly();
			}
			return versionSaved;
		}));
		return applied ? IncrementalVersionResult.APPLIED : IncrementalVersionResult.CONCURRENT_CHANGE;
	}

	// Lee el estado actual de las entidades de la versión y de las que referencian y falten en la
	// copia local (ADR-0009). Por ej: un Profesional que apunta a una Categoria aun no existente
	private VersionContent readContent(long version, VersionChanges changes) {
		Map<Long, Category> categories = new LinkedHashMap<>();
		Map<Long, Professional> professionals = new LinkedHashMap<>();
		Map<Long, WeeklySchedule> schedules = new LinkedHashMap<>();

		if (!readAll(changes.getCategoryIds(), catalogChangesPort::findCategories, Category::getId, categories)
				|| !readAll(changes.getProfessionalIds(), catalogChangesPort::findProfessionals, Professional::getId, professionals)
				|| !readAll(changes.getWeeklyScheduleIds(), catalogChangesPort::findWeeklySchedules, WeeklySchedule::getId, schedules)) {
			log.warn("Catalog version {} lists entities that are not in Redis", version);
			return null; // Null si algo no está en Redis.
		}

		// Los horarios apuntan a un profesional y los profesionales a una categoría. Se completa de
		// abajo hacia arriba: el profesional que falte puede, a su vez, traer una categoría que falte.
		List<Long> professionalRefs = schedules.values().stream().map(WeeklySchedule::getProfessionalId).toList();
		Set<Long> missingProfessionals = missing(
				professionalRefs, professionals.keySet(), professionalRepository.findExistingIds(professionalRefs));
		if (!readAll(missingProfessionals, catalogChangesPort::findProfessionals, Professional::getId, professionals)) {
			log.warn("Catalog version {} references professionals that are not in Redis", version);
			return null;
		}
		List<Long> categoryRefs = professionals.values().stream().map(Professional::getCategoryId).toList();
		Set<Long> missingCategories = missing(categoryRefs, categories.keySet(), categoryRepository.findExistingIds(categoryRefs));
		if (!readAll(missingCategories, catalogChangesPort::findCategories, Category::getId, categories)) {
			log.warn("Catalog version {} references categories that are not in Redis", version);
			return null;
		}
		return new VersionContent(
				List.copyOf(categories.values()), List.copyOf(professionals.values()), List.copyOf(schedules.values()));
	}

	// Los ids referenciados que no vienen en la versión ni están en la copia local.
	private Set<Long> missing(Collection<Long> referenced, Set<Long> inThisVersion, Set<Long> inLocalCopy) {
		Set<Long> missing = new LinkedHashSet<>(referenced);
		missing.removeAll(inThisVersion);
		missing.removeAll(inLocalCopy);
		return missing;
	}

	// Lee las entidades de los ids pedidos y las suma a destino. si alguna no se pudo leer: false
	// ya que el estado no se puede verificar.
	private <T> boolean readAll(Collection<Long> ids, Function<Collection<Long>, List<T>> reader,
			Function<T, Long> idOf, Map<Long, T> destination) {
		Set<Long> wanted = new LinkedHashSet<>(ids);
		if (wanted.isEmpty()) {
			return true;
		}
		List<T> found = new ArrayList<>(reader.apply(wanted));
		found.forEach(entity -> destination.put(idOf.apply(entity), entity));
		return wanted.stream().allMatch(destination::containsKey);
	}
	// record para que sea inmutable
	private record VersionContent(List<Category> categories, List<Professional> professionals, List<WeeklySchedule> schedules) {
	}

}
