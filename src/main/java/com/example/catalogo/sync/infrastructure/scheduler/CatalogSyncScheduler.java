package com.example.catalogo.sync.infrastructure.scheduler;

import com.example.catalogo.sync.application.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Chequeo periódico (ADR-0006): recupera lo que un disparador no pudo, por ejemplo un snapshot que
// falló con la cátedra caída. La primera vez corre un intervalo después del arranque, que ya
// sincronizó. fixedDelay: el próximo cuenta desde que termina el anterior, así nunca se superponen.
@Component
@RequiredArgsConstructor
public class CatalogSyncScheduler {

	private final SyncService syncService;

	@Scheduled(fixedDelayString = "${catalogo.sync.check-interval}", initialDelayString = "${catalogo.sync.check-interval}")
	public void checkPeriodically() {
		syncService.synchronizeCatalog();
	}

}
