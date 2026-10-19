package com.example.catalogo.sync.infrastructure.startup;

import static org.mockito.Mockito.inOrder;

import com.example.catalogo.sync.application.service.SyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogSyncStartupListenerTest {

	@Mock
	private SyncService syncService;

	@InjectMocks
	private CatalogSyncStartupListener listener;

	// Primero se borra la marca que pudo dejar una caída; si fuera al revés, se borraría la del
	// ciclo que acaba de empezar.
	@Test
	void clearsTheInterruptedSyncBeforeSynchronizing() {
		listener.onApplicationReady();

		InOrder order = inOrder(syncService);
		order.verify(syncService).clearInterruptedSync();
		order.verify(syncService).synchronizeCatalog();
	}

}
