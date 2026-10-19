package com.example.catalogo.sync.infrastructure.persistence.mapper;

import com.example.catalogo.sync.domain.model.SyncState;
import com.example.catalogo.sync.infrastructure.persistence.entity.SyncStateEntity;
import org.springframework.stereotype.Component;

@Component
public class SyncStateMapper {

	public SyncStateEntity toEntity(SyncState syncState) {
		if (syncState == null) {
			return null;
		}
		return SyncStateEntity.builder()
				.id(SyncStateEntity.SINGLE_ROW_ID)
				.localVersion(syncState.getLocalVersion())
				.syncInProgress(syncState.isSyncInProgress())
				.lastSuccessAt(syncState.getLastSuccessAt())
				.lastSuccessType(syncState.getLastSuccessType())
				.lastErrorAt(syncState.getLastErrorAt())
				.lastError(syncState.getLastError())
				.build();
	}

	public SyncState toDomainModel(SyncStateEntity entity) {
		if (entity == null) {
			return null;
		}
		return SyncState.builder()
				.localVersion(entity.getLocalVersion())
				.syncInProgress(entity.isSyncInProgress())
				.lastSuccessAt(entity.getLastSuccessAt())
				.lastSuccessType(entity.getLastSuccessType())
				.lastErrorAt(entity.getLastErrorAt())
				.lastError(entity.getLastError())
				.build();
	}

}
