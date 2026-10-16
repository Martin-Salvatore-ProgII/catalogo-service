package com.example.catalogo.sync.infrastructure.startup;

import com.example.catalogo.sync.application.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// Disparador del arranque (ADR-0006). Espera a que la aplicación esté lista: mientras sincroniza,
// el servicio ya atiende pedidos con la copia vigente (ADR-0010).
@Component
@RequiredArgsConstructor
public class CatalogSyncStartupListener {

	private final SyncService syncService;

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		syncService.clearInterruptedSync();
		syncService.synchronizeCatalog();
	}

}
