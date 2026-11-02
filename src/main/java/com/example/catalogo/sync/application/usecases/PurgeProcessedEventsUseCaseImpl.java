package com.example.catalogo.sync.application.usecases;

import java.time.Clock;
import java.time.Duration;

import com.example.catalogo.sync.domain.ports.in.PurgeProcessedEventsUseCase;
import com.example.catalogo.sync.domain.ports.out.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// Sin purga, la tabla crecería con cada aviso para siempre. Un aviso viejo que se repitiera
// después de purgado no hace daño: la decisión sale de comparar versiones con Redis (ADR-0008).
@Slf4j
@Component
@RequiredArgsConstructor
public class PurgeProcessedEventsUseCaseImpl implements PurgeProcessedEventsUseCase {

	private final ProcessedEventRepository processedEventRepository;
	private final Clock clock;

	@Override
	public int purgeProcessedEvents(Duration retention) {
		int deleted = processedEventRepository.deleteProcessedBefore(clock.instant().minus(retention));
		if (deleted > 0) {
			log.info("Purged {} processed catalog events older than {}", deleted, retention);
		}
		return deleted;
	}

}
