package com.example.catalogo.sync.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.catalogo.sync.domain.model.SyncState;
import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IsCatalogReadyUseCaseImplTest {

	@Mock
	private SyncStateRepository syncStateRepository;

	@InjectMocks
	private IsCatalogReadyUseCaseImpl useCase;

	@Test
	void isNotReadyWithoutALocalVersion() {
		when(syncStateRepository.getCurrent()).thenReturn(SyncState.builder().localVersion(null).build());

		assertThat(useCase.isCatalogReady()).isFalse();
	}

	@Test
	void isReadyWithALocalVersion() {
		when(syncStateRepository.getCurrent()).thenReturn(SyncState.builder().localVersion(1L).build());

		assertThat(useCase.isCatalogReady()).isTrue();
	}

	@Test
	void isStillReadyWhileASyncIsInProgress() {
		// Mientras sincroniza, la copia anterior sigue siendo válida (ADR-0010).
		when(syncStateRepository.getCurrent()).thenReturn(SyncState.builder().localVersion(1L).syncInProgress(true).build());

		assertThat(useCase.isCatalogReady()).isTrue();
	}

}
