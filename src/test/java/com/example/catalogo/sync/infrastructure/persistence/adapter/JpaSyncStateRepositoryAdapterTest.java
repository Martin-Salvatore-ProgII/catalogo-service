package com.example.catalogo.sync.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.example.catalogo.TestcontainersConfiguration;
import com.example.catalogo.sync.domain.model.SyncState;
import com.example.catalogo.sync.domain.model.SyncType;
import com.example.catalogo.sync.infrastructure.persistence.mapper.SyncStateMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

// Prueba el adaptador contra PostgreSQL real, con las migraciones de Flyway (ADR-0045): la fila
// inicial la crea la migración. Cada test corre en una transacción que se deshace al terminar.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, JpaSyncStateRepositoryAdapter.class, SyncStateMapper.class })
class JpaSyncStateRepositoryAdapterTest {

	private static final Instant FIRST_SYNC_AT = Instant.parse("2026-10-16T13:00:00Z");
	private static final Instant SECOND_SYNC_AT = Instant.parse("2026-10-16T13:05:00Z");

	@Autowired
	private JpaSyncStateRepositoryAdapter adapter;

	@Test
	void startsWithoutLocalVersionAndWithoutSyncInProgress() {
		SyncState state = adapter.getCurrent();

		assertThat(state.getLocalVersion()).isNull();
		assertThat(state.isSyncInProgress()).isFalse();
		assertThat(state.getLastSuccessAt()).isNull();
		assertThat(state.getLastSuccessType()).isNull();
		assertThat(state.getLastErrorAt()).isNull();
		assertThat(state.getLastError()).isNull();
	}

	@Test
	void marksASyncAsStarted() {
		adapter.markSyncStarted();

		assertThat(adapter.getCurrent().isSyncInProgress()).isTrue();
	}

	@Test
	void recordsTheFirstSuccessWhenThereIsNoLocalVersion() {
		adapter.markSyncStarted();

		boolean applied = adapter.recordSuccessIfVersionIs(null, 1L, SyncType.SNAPSHOT, FIRST_SYNC_AT);

		SyncState state = adapter.getCurrent();
		assertThat(applied).isTrue();
		assertThat(state.getLocalVersion()).isEqualTo(1L);
		assertThat(state.isSyncInProgress()).isFalse();
		assertThat(state.getLastSuccessAt()).isEqualTo(FIRST_SYNC_AT);
		assertThat(state.getLastSuccessType()).isEqualTo(SyncType.SNAPSHOT);
	}

	@Test
	void advancesFromTheExpectedVersion() {
		adapter.recordSuccessIfVersionIs(null, 1L, SyncType.SNAPSHOT, FIRST_SYNC_AT);

		boolean applied = adapter.recordSuccessIfVersionIs(1L, 2L, SyncType.INCREMENTAL, SECOND_SYNC_AT);

		SyncState state = adapter.getCurrent();
		assertThat(applied).isTrue();
		assertThat(state.getLocalVersion()).isEqualTo(2L);
		assertThat(state.getLastSuccessType()).isEqualTo(SyncType.INCREMENTAL);
		assertThat(state.getLastSuccessAt()).isEqualTo(SECOND_SYNC_AT);
	}

	@Test
	void changesNothingWhenAnotherSyncAlreadyChangedTheVersion() {
		// Dos ciclos leyeron la versión 1; el primero ya la llevó a 2 y el segundo llega tarde (ADR-0007).
		adapter.recordSuccessIfVersionIs(null, 1L, SyncType.SNAPSHOT, FIRST_SYNC_AT);
		adapter.recordSuccessIfVersionIs(1L, 2L, SyncType.INCREMENTAL, FIRST_SYNC_AT);

		boolean applied = adapter.recordSuccessIfVersionIs(1L, 2L, SyncType.INCREMENTAL, SECOND_SYNC_AT);

		SyncState state = adapter.getCurrent();
		assertThat(applied).isFalse();
		assertThat(state.getLocalVersion()).isEqualTo(2L);
		assertThat(state.getLastSuccessAt()).isEqualTo(FIRST_SYNC_AT);
	}

	@Test
	void doesNotApplyAFirstSnapshotWhenThereIsAlreadyALocalVersion() {
		// Dos snapshots iniciales a la vez: los dos esperaban "sin versión", pero solo gana uno.
		adapter.recordSuccessIfVersionIs(null, 1L, SyncType.SNAPSHOT, FIRST_SYNC_AT);

		boolean applied = adapter.recordSuccessIfVersionIs(null, 1L, SyncType.SNAPSHOT, SECOND_SYNC_AT);

		assertThat(applied).isFalse();
		assertThat(adapter.getCurrent().getLastSuccessAt()).isEqualTo(FIRST_SYNC_AT);
	}

	@Test
	void recordsAFailureWithoutChangingTheVersion() {
		adapter.recordSuccessIfVersionIs(null, 1L, SyncType.SNAPSHOT, FIRST_SYNC_AT);
		adapter.markSyncStarted();

		adapter.recordFailure("Catedra service unavailable (HttpTimeoutException)", SECOND_SYNC_AT);

		SyncState state = adapter.getCurrent();
		assertThat(state.getLocalVersion()).isEqualTo(1L);
		assertThat(state.isSyncInProgress()).isFalse();
		assertThat(state.getLastError()).isEqualTo("Catedra service unavailable (HttpTimeoutException)");
		assertThat(state.getLastErrorAt()).isEqualTo(SECOND_SYNC_AT);
		assertThat(state.getLastSuccessAt()).isEqualTo(FIRST_SYNC_AT);
	}

	@Test
	void clearsTheSyncInProgressMarkLeftByACrash() {
		adapter.markSyncStarted();

		adapter.clearSyncInProgress();

		assertThat(adapter.getCurrent().isSyncInProgress()).isFalse();
	}

}
