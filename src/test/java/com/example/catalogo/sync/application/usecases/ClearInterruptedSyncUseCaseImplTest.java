package com.example.catalogo.sync.application.usecases;

import static org.mockito.Mockito.verify;

import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClearInterruptedSyncUseCaseImplTest {

	@Mock
	private SyncStateRepository syncStateRepository;

	@InjectMocks
	private ClearInterruptedSyncUseCaseImpl useCase;

	@Test
	void clearsTheSyncInProgressMark() {
		useCase.clearInterruptedSync();

		verify(syncStateRepository).clearSyncInProgress();
	}

}
