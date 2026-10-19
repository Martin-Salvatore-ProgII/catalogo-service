package com.example.catalogo.sync.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.professional.domain.ports.out.ProfessionalRepository;
import com.example.catalogo.professional.domain.ports.out.WeeklyScheduleRepository;
import com.example.catalogo.sync.application.exception.CatalogSnapshotException;
import com.example.catalogo.sync.domain.model.CatalogSnapshot;
import com.example.catalogo.sync.domain.model.SyncState;
import com.example.catalogo.sync.domain.model.SyncType;
import com.example.catalogo.sync.domain.ports.out.CatalogSnapshotPort;
import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;

// Las reglas del ciclo de sincronización (CU-01, CU-02), sin Spring ni base: los puertos son mocks
// y la transacción es una falsa que registra si se pidió deshacerla.
@ExtendWith(MockitoExtension.class)
class SynchronizeCatalogUseCaseImplTest {

	private static final Instant NOW = Instant.parse("2026-10-16T13:00:00Z");

	@Mock
	private SyncStateRepository syncStateRepository;
	@Mock
	private CatalogSnapshotPort catalogSnapshotPort;
	@Mock
	private CategoryRepository categoryRepository;
	@Mock
	private ProfessionalRepository professionalRepository;
	@Mock
	private WeeklyScheduleRepository weeklyScheduleRepository;

	private final RecordingTransaction transaction = new RecordingTransaction();

	private SynchronizeCatalogUseCaseImpl useCase;

	@BeforeEach
	void setUp() {
		useCase = new SynchronizeCatalogUseCaseImpl(syncStateRepository, catalogSnapshotPort, categoryRepository,
				professionalRepository, weeklyScheduleRepository, transaction, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void doesNothingWhenThereIsAlreadyALocalVersion() {
		givenLocalVersion(3L);

		useCase.synchronizeCatalog();

		verifyNoInteractions(catalogSnapshotPort, categoryRepository, professionalRepository, weeklyScheduleRepository);
		verify(syncStateRepository, never()).markSyncStarted();
	}

	@Test
	void rebuildsTheCatalogFromTheSnapshotWhenThereIsNoLocalVersion() {
		givenLocalVersion(null);
		CatalogSnapshot snapshot = snapshot(1L);
		when(catalogSnapshotPort.getSnapshot()).thenReturn(snapshot);
		when(syncStateRepository.recordSuccessIfVersionIs(null, 1L, SyncType.SNAPSHOT, NOW)).thenReturn(true);

		useCase.synchronizeCatalog();

		// Se marca el inicio, se pide el snapshot fuera de la transacción y adentro se borra de hijos
		// a padres, se guarda de padres a hijos y por último la versión.
		InOrder order = inOrder(syncStateRepository, catalogSnapshotPort, weeklyScheduleRepository,
				professionalRepository, categoryRepository);
		order.verify(syncStateRepository).markSyncStarted();
		order.verify(catalogSnapshotPort).getSnapshot();
		order.verify(weeklyScheduleRepository).deleteAll();
		order.verify(professionalRepository).deleteAll();
		order.verify(categoryRepository).deleteAll();
		order.verify(categoryRepository).saveAll(snapshot.getCategories());
		order.verify(professionalRepository).saveAll(snapshot.getProfessionals());
		order.verify(weeklyScheduleRepository).saveAll(snapshot.getWeeklySchedules());
		order.verify(syncStateRepository).recordSuccessIfVersionIs(null, 1L, SyncType.SNAPSHOT, NOW);
		assertThat(transaction.executions).isEqualTo(1);
		assertThat(transaction.rolledBack).isFalse();
		verify(syncStateRepository, never()).recordFailure(any(), any());
	}

	@Test
	void asksTheCatedraBeforeOpeningTheTransaction() {
		givenLocalVersion(null);
		when(catalogSnapshotPort.getSnapshot()).thenAnswer(invocation -> {
			assertThat(transaction.executions).isZero();
			return snapshot(1L);
		});
		when(syncStateRepository.recordSuccessIfVersionIs(null, 1L, SyncType.SNAPSHOT, NOW)).thenReturn(true);

		useCase.synchronizeCatalog();

		assertThat(transaction.executions).isEqualTo(1);
	}

	@Test
	void discardsTheSnapshotWhenAnotherSyncAlreadyChangedTheVersion() {
		// CU-02, 3a: la versión local ya no es la leída al empezar.
		givenLocalVersion(null);
		when(catalogSnapshotPort.getSnapshot()).thenReturn(snapshot(1L));
		when(syncStateRepository.recordSuccessIfVersionIs(null, 1L, SyncType.SNAPSHOT, NOW)).thenReturn(false);

		useCase.synchronizeCatalog();

		assertThat(transaction.rolledBack).isTrue();
		verify(syncStateRepository).clearSyncInProgress();
		verify(syncStateRepository, never()).recordFailure(any(), any());
	}

	@Test
	void recordsTheCatedraFailureWithoutTouchingTheCopy() {
		// CU-02, 1a y 2a: la copia queda intacta y el error se registra con el mensaje seguro.
		givenLocalVersion(null);
		when(catalogSnapshotPort.getSnapshot())
				.thenThrow(new CatalogSnapshotException("Catedra service unavailable (HttpTimeoutException)"));

		useCase.synchronizeCatalog();

		verify(syncStateRepository).recordFailure("Catedra service unavailable (HttpTimeoutException)", NOW);
		verifyNoInteractions(categoryRepository, professionalRepository, weeklyScheduleRepository);
		assertThat(transaction.executions).isZero();
	}

	@Test
	void recordsAnInternalFailureWhenTheCopyCannotBeStored() {
		givenLocalVersion(null);
		CatalogSnapshot snapshot = snapshot(1L);
		when(catalogSnapshotPort.getSnapshot()).thenReturn(snapshot);
		doThrow(new DataIntegrityViolationException("insert or update on table \"professional\" violates foreign key"))
				.when(professionalRepository).saveAll(snapshot.getProfessionals());

		useCase.synchronizeCatalog();

		verify(syncStateRepository).recordFailure(
				"Internal error: catalog snapshot could not be stored (DataIntegrityViolationException)", NOW);
		verify(syncStateRepository, never()).recordSuccessIfVersionIs(any(), anyLong(), any(), any());
	}

	private void givenLocalVersion(Long version) {
		when(syncStateRepository.getCurrent()).thenReturn(SyncState.builder().localVersion(version).build());
	}

	private static CatalogSnapshot snapshot(long version) {
		return CatalogSnapshot.builder()
				.snapshotVersion(version)
				.categories(List.of(Category.builder().id(101L).build()))
				.professionals(List.of(Professional.builder().id(201L).categoryId(101L).build()))
				.weeklySchedules(List.of(WeeklySchedule.builder().id(301L).professionalId(201L).build()))
				.build();
	}

	// Ejecuta el bloque como lo haría Spring y registra si se pidió deshacer la transacción. Si el
	// bloque lanza una excepción, la deja pasar, como TransactionTemplate.
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
