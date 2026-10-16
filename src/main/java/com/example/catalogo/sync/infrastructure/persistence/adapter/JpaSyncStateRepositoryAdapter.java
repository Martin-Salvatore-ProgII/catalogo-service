package com.example.catalogo.sync.infrastructure.persistence.adapter;

import java.time.Instant;

import com.example.catalogo.sync.domain.model.SyncState;
import com.example.catalogo.sync.domain.model.SyncType;
import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import com.example.catalogo.sync.infrastructure.persistence.entity.SyncStateEntity;
import com.example.catalogo.sync.infrastructure.persistence.mapper.SyncStateMapper;
import com.example.catalogo.sync.infrastructure.persistence.repository.JpaSyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaSyncStateRepositoryAdapter implements SyncStateRepository {

	private final JpaSyncStateRepository jpaSyncStateRepository;
	private final SyncStateMapper syncStateMapper;

	// La fila la crea la migración; si falta, la base está rota y no hay estado que informar.
	@Override
	public SyncState getCurrent() {
		return jpaSyncStateRepository.findById(SyncStateEntity.SINGLE_ROW_ID)
				.map(syncStateMapper::toDomainModel)
				.orElseThrow(() -> new IllegalStateException("The sync_state row is missing"));
	}

	@Override
	public void markSyncStarted() {
		jpaSyncStateRepository.markSyncStarted();
	}

	@Override
	public boolean recordSuccessIfVersionIs(Long expectedVersion, long newVersion, SyncType type, Instant succeededAt) {
		return jpaSyncStateRepository.updateVersionIfCurrentIs(expectedVersion, newVersion, type.name(), succeededAt) == 1;
	}

	@Override
	public void recordFailure(String error, Instant failedAt) {
		jpaSyncStateRepository.recordFailure(error, failedAt);
	}

	@Override
	public void clearSyncInProgress() {
		jpaSyncStateRepository.clearSyncInProgress();
	}

}
