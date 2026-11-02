package com.example.catalogo.sync.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.example.catalogo.sync.domain.ports.out.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterProcessedEventUseCaseImplTest {

	private static final Instant NOW = Instant.parse("2026-11-02T15:00:00Z");

	@Mock
	private ProcessedEventRepository processedEventRepository;

	private RegisterProcessedEventUseCaseImpl useCase;

	@BeforeEach
	void setUp() {
		useCase = new RegisterProcessedEventUseCaseImpl(processedEventRepository, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void registersANewEventWithTheCurrentTimeAndSaysItIsNew() {
		when(processedEventRepository.registerIfNew("evento-1", NOW)).thenReturn(true);

		assertThat(useCase.registerProcessedEvent("evento-1")).isTrue();

		verify(processedEventRepository).registerIfNew("evento-1", NOW);
	}

	@Test
	void recognizesADuplicateEvent() {
		when(processedEventRepository.registerIfNew("evento-1", NOW)).thenReturn(false);

		assertThat(useCase.registerProcessedEvent("evento-1")).isFalse();
	}

}
