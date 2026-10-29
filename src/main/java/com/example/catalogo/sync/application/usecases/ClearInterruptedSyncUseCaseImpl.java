package com.example.catalogo.sync.application.usecases;

import com.example.catalogo.sync.domain.ports.in.ClearInterruptedSyncUseCase;
import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Un proceso que recién arranca no tiene ningún ciclo en curso: si quedó la marca, es de una
// sincronización que una caída dejó a medias (docs/arq/robustez.md).
@Component
@RequiredArgsConstructor
public class ClearInterruptedSyncUseCaseImpl implements ClearInterruptedSyncUseCase {

	private final SyncStateRepository syncStateRepository;

	@Override
	public void clearInterruptedSync() {
		syncStateRepository.clearSyncInProgress();
	}

}
