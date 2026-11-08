package com.example.catalogo.sync.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.example.catalogo.sync.domain.ports.out.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurgeProcessedEventsUseCaseImplTest {

	private static final Instant NOW = Instant.parse("2026-11-02T15:00:00Z");

	@Mock
	private ProcessedEventRepository processedEventRepository;

	private PurgeProcessedEventsUseCaseImpl useCase;

	@BeforeEach
	void setUp() {
		useCase = new PurgeProcessedEventsUseCaseImpl(processedEventRepository, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void deletesTheEventsOlderThanTheRetentionCountedFromNow() {
		when(processedEventRepository.deleteProcessedBefore(Instant.parse("2026-10-26T15:00:00Z"))).thenReturn(4);

		assertThat(useCase.purgeProcessedEvents(Duration.ofDays(7))).isEqualTo(4);

		verify(processedEventRepository).deleteProcessedBefore(Instant.parse("2026-10-26T15:00:00Z"));
	}

}
