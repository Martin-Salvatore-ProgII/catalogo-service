package com.example.catalogo.sync.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.professional.domain.ports.out.ProfessionalRepository;
import com.example.catalogo.professional.domain.ports.out.WeeklyScheduleRepository;
import com.example.catalogo.sync.application.exception.CatalogChangesException;
import com.example.catalogo.sync.domain.model.SyncType;
import com.example.catalogo.sync.domain.model.VersionChanges;
import com.example.catalogo.sync.domain.ports.out.CatalogChangesPort;
import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

// Aplicar una versión incremental (CU-01, paso 4; ADR-0007, ADR-0009), sin Spring ni base: los
// puertos son mocks y la transacción es una falsa que registra si se pidió deshacerla.
@ExtendWith(MockitoExtension.class)
class IncrementalVersionApplierTest {

	private static final Instant NOW = Instant.parse("2026-11-02T15:00:00Z");

	@Mock
	private CatalogChangesPort changesPort;
	@Mock
	private SyncStateRepository syncStateRepository;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private ProfessionalRepository professionalRepository;
	@Mock
	private WeeklyScheduleRepository weeklyScheduleRepository;

	private RecordingTransaction transaction;
	private IncrementalVersionApplier applier;

	@BeforeEach
	void setUp() {
		transaction = new RecordingTransaction();
		applier = new IncrementalVersionApplier(changesPort, syncStateRepository, categoryRepository,
				professionalRepository, weeklyScheduleRepository, transaction, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void savesTheChangesAndAdvancesTheVersionInOneTransaction() {
		whenChanges(8, List.of(), List.of(201L), List.of(301L));
		when(changesPort.findProfessionals(Set.of(201L))).thenReturn(List.of(professional(201, 101)));
		when(changesPort.findWeeklySchedules(Set.of(301L))).thenReturn(List.of(schedule(301, 201)));
		when(categoryRepository.findExistingIds(anyCollection())).thenReturn(Set.of(101L));
		when(syncStateRepository.recordSuccessIfVersionIs(7L, 8L, SyncType.INCREMENTAL, NOW)).thenReturn(true);

		assertThat(applier.apply(8)).isEqualTo(IncrementalVersionResult.APPLIED);

		verify(professionalRepository).saveAll(List.of(professional(201, 101)));
		verify(weeklyScheduleRepository).saveAll(List.of(schedule(301, 201)));
		assertThat(transaction.executions).isEqualTo(1);
		assertThat(transaction.rolledBack).isFalse();
	}

	@Test
	void savesDisabledEntitiesToo() {
		whenChanges(8, List.of(101L), List.of(), List.of());
		Category disabled = Category.builder().id(101L).enabled(false).build();
		when(changesPort.findCategories(Set.of(101L))).thenReturn(List.of(disabled));
		when(syncStateRepository.recordSuccessIfVersionIs(7L, 8L, SyncType.INCREMENTAL, NOW)).thenReturn(true);

		applier.apply(8);

		verify(categoryRepository).saveAll(List.of(disabled));
	}

	@Test
	void fetchesFromRedisTheCategoryAProfessionalPointsToWhenTheLocalCopyDoesNotHaveIt() {
		// ADR-0009: el profesional apunta a la categoría 102, creada en una versión posterior.
		whenChanges(8, List.of(), List.of(201L), List.of());
		when(changesPort.findProfessionals(Set.of(201L))).thenReturn(List.of(professional(201, 102)));
		when(categoryRepository.findExistingIds(List.of(102L))).thenReturn(Set.of());
		when(changesPort.findCategories(Set.of(102L))).thenReturn(List.of(category(102)));
		when(syncStateRepository.recordSuccessIfVersionIs(7L, 8L, SyncType.INCREMENTAL, NOW)).thenReturn(true);

		assertThat(applier.apply(8)).isEqualTo(IncrementalVersionResult.APPLIED);

		verify(categoryRepository).saveAll(List.of(category(102)));
	}

	@Test
	void fetchesTheProfessionalAndItsCategoryWhenAScheduleReferencesOnesTheLocalCopyDoesNotHave() {
		whenChanges(8, List.of(), List.of(), List.of(301L));
		when(changesPort.findWeeklySchedules(Set.of(301L))).thenReturn(List.of(schedule(301, 202)));
		when(professionalRepository.findExistingIds(List.of(202L))).thenReturn(Set.of());
		when(changesPort.findProfessionals(Set.of(202L))).thenReturn(List.of(professional(202, 103)));
		when(categoryRepository.findExistingIds(List.of(103L))).thenReturn(Set.of());
		when(changesPort.findCategories(Set.of(103L))).thenReturn(List.of(category(103)));
		when(syncStateRepository.recordSuccessIfVersionIs(7L, 8L, SyncType.INCREMENTAL, NOW)).thenReturn(true);

		assertThat(applier.apply(8)).isEqualTo(IncrementalVersionResult.APPLIED);

		verify(categoryRepository).saveAll(List.of(category(103)));
		verify(professionalRepository).saveAll(List.of(professional(202, 103)));
	}

	@Test
	void doesNotFetchAReferenceThatTheLocalCopyAlreadyHas() {
		whenChanges(8, List.of(), List.of(201L), List.of());
		when(changesPort.findProfessionals(Set.of(201L))).thenReturn(List.of(professional(201, 101)));
		when(categoryRepository.findExistingIds(List.of(101L))).thenReturn(Set.of(101L));
		when(syncStateRepository.recordSuccessIfVersionIs(7L, 8L, SyncType.INCREMENTAL, NOW)).thenReturn(true);

		applier.apply(8);

		verify(changesPort, never()).findCategories(anyCollection());
	}

	@Test
	void doesNotFetchAReferenceThatComesInTheSameVersion() {
		whenChanges(8, List.of(102L), List.of(201L), List.of());
		when(changesPort.findCategories(Set.of(102L))).thenReturn(List.of(category(102)));
		when(changesPort.findProfessionals(Set.of(201L))).thenReturn(List.of(professional(201, 102)));
		when(categoryRepository.findExistingIds(List.of(102L))).thenReturn(Set.of());
		when(syncStateRepository.recordSuccessIfVersionIs(7L, 8L, SyncType.INCREMENTAL, NOW)).thenReturn(true);

		assertThat(applier.apply(8)).isEqualTo(IncrementalVersionResult.APPLIED);

		verify(changesPort).findCategories(Set.of(102L));
	}

	@Test
	void isUnverifiableWhenTheVersionHasNoChanges() {
		when(changesPort.findChanges(8)).thenReturn(Optional.empty());

		assertThat(applier.apply(8)).isEqualTo(IncrementalVersionResult.UNVERIFIABLE);

		assertNothingSaved();
	}

	@Test
	void isUnverifiableWhenAListedIdIsNotInItsHash() {
		whenChanges(8, List.of(), List.of(201L, 202L), List.of());
		when(changesPort.findProfessionals(Set.of(201L, 202L))).thenReturn(List.of(professional(201, 101)));

		assertThat(applier.apply(8)).isEqualTo(IncrementalVersionResult.UNVERIFIABLE);

		assertNothingSaved();
	}

	@Test
	void isUnverifiableWhenAReferencedEntityIsNotInRedisEither() {
		whenChanges(8, List.of(), List.of(201L), List.of());
		when(changesPort.findProfessionals(Set.of(201L))).thenReturn(List.of(professional(201, 102)));
		when(categoryRepository.findExistingIds(List.of(102L))).thenReturn(Set.of());
		when(changesPort.findCategories(Set.of(102L))).thenReturn(List.of());

		assertThat(applier.apply(8)).isEqualTo(IncrementalVersionResult.UNVERIFIABLE);

		assertNothingSaved();
	}

	@Test
	void undoesEverythingWhenAnotherCycleAlreadyChangedTheLocalVersion() {
		whenChanges(8, List.of(101L), List.of(), List.of());
		when(changesPort.findCategories(Set.of(101L))).thenReturn(List.of(category(101)));
		when(syncStateRepository.recordSuccessIfVersionIs(7L, 8L, SyncType.INCREMENTAL, NOW)).thenReturn(false);

		assertThat(applier.apply(8)).isEqualTo(IncrementalVersionResult.CONCURRENT_CHANGE);

		assertThat(transaction.rolledBack).isTrue();
	}

	@Test
	void letsTheRedisFailureReachTheCycleWithoutTouchingTheDatabase() {
		when(changesPort.findChanges(8)).thenThrow(new CatalogChangesException("Catedra Redis unavailable (X)"));

		assertThatThrownBy(() -> applier.apply(8)).isInstanceOf(CatalogChangesException.class);

		assertThat(transaction.executions).isZero();
	}

	private void assertNothingSaved() {
		assertThat(transaction.executions).isZero();
		verify(categoryRepository, never()).saveAll(anyList());
		verify(professionalRepository, never()).saveAll(anyList());
		verify(weeklyScheduleRepository, never()).saveAll(anyList());
		verify(syncStateRepository, never()).recordSuccessIfVersionIs(any(), anyLong(), any(), any());
	}

	private void whenChanges(long version, List<Long> categories, List<Long> professionals, List<Long> schedules) {
		when(changesPort.findChanges(version)).thenReturn(Optional.of(VersionChanges.builder().version(version)
				.categoryIds(categories).professionalIds(professionals).weeklyScheduleIds(schedules).build()));
	}

	// Los modelos no definen equals: el test necesita la misma instancia cada vez que nombra la misma
	// entidad, para poder comparar lo que se guardó con lo que se leyó.
	private final Map<String, Object> entities = new HashMap<>();

	private Category category(long id) {
		return (Category) entities.computeIfAbsent("c" + id,
				k -> Category.builder().id(id).name("Categoria " + id).enabled(true).build());
	}

	private Professional professional(long id, long categoryId) {
		return (Professional) entities.computeIfAbsent("p" + id,
				k -> Professional.builder().id(id).categoryId(categoryId).firstName("Ana").lastName("Gomez").enabled(true).build());
	}

	private WeeklySchedule schedule(long id, long professionalId) {
		return (WeeklySchedule) entities.computeIfAbsent("s" + id,
				k -> WeeklySchedule.builder().id(id).professionalId(professionalId).enabled(true).build());
	}

	private static final class RecordingTransaction implements TransactionOperations {

		private int executions;
		private boolean rolledBack;

		@Override
		public <T> T execute(TransactionCallback<T> action) {
			executions++;
			TransactionStatus status = new SimpleTransactionStatus();
			T result = action.doInTransaction(status);
			rolledBack = status.isRollbackOnly();
			return result;
		}

	}

}
