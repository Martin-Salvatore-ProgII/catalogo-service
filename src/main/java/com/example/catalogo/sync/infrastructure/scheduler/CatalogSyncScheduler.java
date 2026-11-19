package com.example.catalogo.sync.infrastructure.scheduler;

import com.example.catalogo.shared.infrastructure.config.SyncProperties;
import com.example.catalogo.sync.application.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Chequeo periódico (ADR-0006): recupera lo que un disparador no pudo, por ejemplo un snapshot que
// falló con la cátedra caída. La primera vez corre un intervalo después del arranque, que ya
// sincronizó. fixedDelay: el próximo cuenta desde que termina el anterior, así nunca se superponen.
// Aprovecha el mismo momento para purgar los avisos procesados viejos (ADR-0008), después de
// sincronizar: si la purga falla, la sincronización ya se hizo.
@Component
@RequiredArgsConstructor
public class CatalogSyncScheduler {

	private final SyncService syncService;
	private final SyncProperties syncProperties;

	// schedule manejado por Spring . Valores determinados en application.yaml
	@Scheduled(fixedDelayString = "${catalogo.sync.check-interval}", initialDelayString = "${catalogo.sync.check-interval}")
	public void checkPeriodically() {
		syncService.synchronizeCatalog();
		syncService.purgeProcessedEvents(syncProperties.processedEventRetention());
	}

}
