package com.example.catalogo.sync.application.service;

import static org.mockito.Mockito.verify;

import com.example.catalogo.sync.domain.ports.in.ClearInterruptedSyncUseCase;
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

	@InjectMocks
	private SyncService syncService;

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

}
