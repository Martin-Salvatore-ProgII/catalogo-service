package com.example.catalogo.sync.infrastructure.scheduler;

import static org.mockito.Mockito.inOrder;

import java.time.Duration;

import com.example.catalogo.shared.infrastructure.config.SyncProperties;
import com.example.catalogo.sync.application.service.SyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogSyncSchedulerTest {

	@Mock
	private SyncService syncService;

	// Primero se sincroniza: si la purga fallara, la sincronización ya está hecha.
	@Test
	void synchronizesAndThenPurgesWithTheConfiguredRetention() {
		SyncProperties properties = new SyncProperties(Duration.ofMinutes(5), Duration.ofDays(7));

		new CatalogSyncScheduler(syncService, properties).checkPeriodically();

		InOrder order = inOrder(syncService);
		order.verify(syncService).synchronizeCatalog();
		order.verify(syncService).purgeProcessedEvents(Duration.ofDays(7));
	}

}
