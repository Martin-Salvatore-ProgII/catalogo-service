package com.example.catalogo.sync.application.service;

import java.time.Duration;

import com.example.catalogo.sync.application.exception.CatalogNotReadyException;
import com.example.catalogo.sync.domain.model.SyncState;
import com.example.catalogo.sync.domain.ports.in.ClearInterruptedSyncUseCase;
import com.example.catalogo.sync.domain.ports.in.GetSyncStatusUseCase;
import com.example.catalogo.sync.domain.ports.in.IsCatalogReadyUseCase;
import com.example.catalogo.sync.domain.ports.in.PurgeProcessedEventsUseCase;
import com.example.catalogo.sync.domain.ports.in.RegisterProcessedEventUseCase;
import com.example.catalogo.sync.domain.ports.in.SynchronizeCatalogUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SyncService {

	private final SynchronizeCatalogUseCase synchronizeCatalogUseCase;
	private final ClearInterruptedSyncUseCase clearInterruptedSyncUseCase;
	private final IsCatalogReadyUseCase isCatalogReadyUseCase;
	private final GetSyncStatusUseCase getSyncStatusUseCase;
	private final RegisterProcessedEventUseCase registerProcessedEventUseCase;
	private final PurgeProcessedEventsUseCase purgeProcessedEventsUseCase;

	public void synchronizeCatalog() {
		synchronizeCatalogUseCase.synchronizeCatalog();
	}

	public void clearInterruptedSync() {
		clearInterruptedSyncUseCase.clearInterruptedSync();
	}

	// Como en la fachada de referencia: el caso de uso responde y la fachada traduce el "no" a la
	// excepción de aplicación.
	public void requireCatalogReady() {
		if (!isCatalogReadyUseCase.isCatalogReady()) {
			throw new CatalogNotReadyException();
		}
	}

	public SyncState getSyncStatus() {
		return getSyncStatusUseCase.getSyncStatus();
	}

	public boolean registerProcessedEvent(String eventId) {
		return registerProcessedEventUseCase.registerProcessedEvent(eventId);
	}

	public int purgeProcessedEvents(Duration retention) {
		return purgeProcessedEventsUseCase.purgeProcessedEvents(retention);
	}

}
