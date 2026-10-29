package com.example.catalogo.sync.application.usecases;

import com.example.catalogo.sync.domain.model.SyncState;
import com.example.catalogo.sync.domain.ports.in.GetSyncStatusUseCase;
import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetSyncStatusUseCaseImpl implements GetSyncStatusUseCase {

	private final SyncStateRepository syncStateRepository;

	@Override
	public SyncState getSyncStatus() {
		return syncStateRepository.getCurrent();
	}

}
