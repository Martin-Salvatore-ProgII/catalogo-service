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
class GetSyncStatusUseCaseImplTest {

	@Mock
	private SyncStateRepository syncStateRepository;

	@InjectMocks
	private GetSyncStatusUseCaseImpl useCase;

	@Test
	void returnsTheCurrentSyncState() {
		SyncState state = SyncState.builder().localVersion(1L).build();
		when(syncStateRepository.getCurrent()).thenReturn(state);

		assertThat(useCase.getSyncStatus()).isSameAs(state);
	}

}
