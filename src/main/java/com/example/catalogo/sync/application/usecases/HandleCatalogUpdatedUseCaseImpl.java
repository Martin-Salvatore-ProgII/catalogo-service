package com.example.catalogo.sync.application.usecases;

import java.time.Clock;

import com.example.catalogo.sync.domain.ports.in.HandleCatalogUpdatedUseCase;
import com.example.catalogo.sync.domain.ports.in.SynchronizeCatalogUseCase;
import com.example.catalogo.sync.domain.ports.out.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * CU-01, paso 1 y flujos 1a y 1b: un aviso de Kafka es solo un disparador. Si ya se procesó se
 * ignora; si no, se registra y se inicia el ciclo de sincronización, que decide comparando con
 * Redis y nunca con el número del aviso (ADR-0006, ADR-0008).
 *
 * <p>Usa el puerto de entrada del ciclo y no su clase: el ciclo es el mismo caso de uso que
 * disparan el arranque y el chequeo periódico.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HandleCatalogUpdatedUseCaseImpl implements HandleCatalogUpdatedUseCase {

	private final ProcessedEventRepository processedEventRepository;
	private final SynchronizeCatalogUseCase synchronizeCatalogUseCase;
	private final Clock clock;

	@Override
	public void handleCatalogUpdated(String eventId) {
		if (eventId == null) {
			// Sin eventId no hay con qué deduplicar. Sincronizar de más es inofensivo: si la copia
			// ya está al día, el ciclo no hace nada.
			log.warn("Unreadable CatalogUpdated notice: synchronizing anyway");
			synchronizeCatalogUseCase.synchronizeCatalog();
			return;
		}
		// Lo decide la clave primaria de la base (ADR-0070), no una consulta previa.
		if (!processedEventRepository.registerIfNew(eventId, clock.instant())) {
			// Queda a la vista que el duplicado se reconoció (ENUNCIADO §11, REF §18.1).
			log.info("Duplicate CatalogUpdated event {} ignored", eventId);
			return;
		}
		synchronizeCatalogUseCase.synchronizeCatalog();
	}

}
