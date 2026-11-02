package com.example.catalogo.sync.application.usecases;

import java.time.Clock;

import com.example.catalogo.sync.domain.ports.in.RegisterProcessedEventUseCase;
import com.example.catalogo.sync.domain.ports.out.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterProcessedEventUseCaseImpl implements RegisterProcessedEventUseCase {

	private final ProcessedEventRepository processedEventRepository;
	private final Clock clock;

	@Override
	public boolean registerProcessedEvent(String eventId) {
		boolean isNew = processedEventRepository.registerIfNew(eventId, clock.instant());
		if (!isNew) {
			// Queda a la vista que el duplicado se reconoció (ENUNCIADO §11, REF §18.1).
			log.info("Duplicate catalog event {} ignored", eventId);
		}
		return isNew;
	}

}
