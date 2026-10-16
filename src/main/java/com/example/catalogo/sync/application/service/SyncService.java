package com.example.catalogo.sync.application.service;

import com.example.catalogo.sync.domain.ports.in.ClearInterruptedSyncUseCase;
import com.example.catalogo.sync.domain.ports.in.SynchronizeCatalogUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SyncService {

	private final SynchronizeCatalogUseCase synchronizeCatalogUseCase;
	private final ClearInterruptedSyncUseCase clearInterruptedSyncUseCase;

	public void synchronizeCatalog() {
		synchronizeCatalogUseCase.synchronizeCatalog();
	}

	public void clearInterruptedSync() {
		clearInterruptedSyncUseCase.clearInterruptedSync();
	}

}
