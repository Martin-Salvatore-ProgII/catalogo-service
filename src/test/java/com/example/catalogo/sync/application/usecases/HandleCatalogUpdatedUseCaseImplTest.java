package com.example.catalogo.sync.application.usecases;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.example.catalogo.sync.domain.ports.in.SynchronizeCatalogUseCase;
import com.example.catalogo.sync.domain.ports.out.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// CU-01, paso 1 y flujos 1a y 1b, sin Spring ni base.
@ExtendWith(MockitoExtension.class)
class HandleCatalogUpdatedUseCaseImplTest {

	private static final Instant NOW = Instant.parse("2026-11-02T15:00:00Z");

	@Mock
	private ProcessedEventRepository processedEventRepository;
	@Mock
	private SynchronizeCatalogUseCase synchronizeCatalogUseCase;

	private HandleCatalogUpdatedUseCaseImpl useCase;

	@BeforeEach
	void setUp() {
		useCase = new HandleCatalogUpdatedUseCaseImpl(processedEventRepository, synchronizeCatalogUseCase,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void registersANewEventAndThenSynchronizes() {
		when(processedEventRepository.registerIfNew("evento-1", NOW)).thenReturn(true);

		useCase.handleCatalogUpdated("evento-1");

		InOrder order = inOrder(processedEventRepository, synchronizeCatalogUseCase);
		order.verify(processedEventRepository).registerIfNew("evento-1", NOW);
		order.verify(synchronizeCatalogUseCase).synchronizeCatalog();
	}

	@Test
	void ignoresADuplicateEventWithoutSynchronizing() {
		// CU-01, 1a (REF §18.1).
		when(processedEventRepository.registerIfNew("evento-1", NOW)).thenReturn(false);

		useCase.handleCatalogUpdated("evento-1");

		verifyNoInteractions(synchronizeCatalogUseCase);
	}

	@Test
	void synchronizesAnywayWhenTheNoticeHasNoEventId() {
		// CU-01, 1b: la decisión no depende del contenido del aviso.
		useCase.handleCatalogUpdated(null);

		verify(synchronizeCatalogUseCase).synchronizeCatalog();
		verifyNoInteractions(processedEventRepository);
	}

}
