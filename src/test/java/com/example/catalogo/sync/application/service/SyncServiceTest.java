package com.example.catalogo.sync.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import com.example.catalogo.sync.application.exception.CatalogNotReadyException;
import com.example.catalogo.sync.domain.model.SyncState;
import com.example.catalogo.sync.domain.ports.in.ClearInterruptedSyncUseCase;
import com.example.catalogo.sync.domain.ports.in.GetSyncStatusUseCase;
import com.example.catalogo.sync.domain.ports.in.HandleCatalogUpdatedUseCase;
import com.example.catalogo.sync.domain.ports.in.IsCatalogReadyUseCase;
import com.example.catalogo.sync.domain.ports.in.PurgeProcessedEventsUseCase;
import com.example.catalogo.sync.domain.ports.in.SynchronizeCatalogUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

	@Mock
	private SynchronizeCatalogUseCase synchronizeCatalogUseCase;

	@Mock
	private ClearInterruptedSyncUseCase clearInterruptedSyncUseCase;

	@Mock
	private IsCatalogReadyUseCase isCatalogReadyUseCase;

	@Mock
	private GetSyncStatusUseCase getSyncStatusUseCase;

	@Mock
	private HandleCatalogUpdatedUseCase handleCatalogUpdatedUseCase;

	@Mock
	private PurgeProcessedEventsUseCase purgeProcessedEventsUseCase;

	@InjectMocks
	private SyncService syncService;

	@Test
	void requireCatalogReadyFailsWhenTheCatalogIsNotReady() {
		when(isCatalogReadyUseCase.isCatalogReady()).thenReturn(false);

		assertThatThrownBy(syncService::requireCatalogReady).isInstanceOf(CatalogNotReadyException.class);
	}

	@Test
	void requireCatalogReadyPassesWhenTheCatalogIsReady() {
		when(isCatalogReadyUseCase.isCatalogReady()).thenReturn(true);

		assertThatCode(syncService::requireCatalogReady).doesNotThrowAnyException();
	}

	@Test
	void getSyncStatusDelegatesToTheUseCase() {
		SyncState state = SyncState.builder().localVersion(1L).build();
		when(getSyncStatusUseCase.getSyncStatus()).thenReturn(state);

		assertThat(syncService.getSyncStatus()).isSameAs(state);
	}

	@Test
	void synchronizeCatalogDelegatesToTheUseCase() {
		syncService.synchronizeCatalog();

		verify(synchronizeCatalogUseCase).synchronizeCatalog();
	}

	@Test
	void clearInterruptedSyncDelegatesToTheUseCase() {
		syncService.clearInterruptedSync();

		verify(clearInterruptedSyncUseCase).clearInterruptedSync();
	}

	@Test
	void handlesACatalogUpdatedNoticeThroughTheUseCase() {
		syncService.handleCatalogUpdated("evento-1");

		verify(handleCatalogUpdatedUseCase).handleCatalogUpdated("evento-1");
	}

	@Test
	void purgesProcessedEventsThroughTheUseCase() {
		when(purgeProcessedEventsUseCase.purgeProcessedEvents(Duration.ofDays(7))).thenReturn(3);

		assertThat(syncService.purgeProcessedEvents(Duration.ofDays(7))).isEqualTo(3);
	}

}
