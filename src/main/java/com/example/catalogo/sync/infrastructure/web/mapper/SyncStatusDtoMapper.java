package com.example.catalogo.sync.infrastructure.web.mapper;

import com.example.catalogo.sync.domain.model.SyncState;
import com.example.catalogo.sync.infrastructure.web.dto.SyncStatusResponse;
import org.springframework.stereotype.Component;

@Component
public class SyncStatusDtoMapper {

	public SyncStatusResponse toResponse(SyncState syncState) {
		if (syncState == null) {
			return null;
		}
		return SyncStatusResponse.builder()
				.localVersion(syncState.getLocalVersion())
				.syncInProgress(syncState.isSyncInProgress())
				.lastSuccessAt(syncState.getLastSuccessAt())
				.lastSuccessType(syncState.getLastSuccessType() == null ? null : syncState.getLastSuccessType().name())
				.lastErrorAt(syncState.getLastErrorAt())
				.lastError(syncState.getLastError())
				.build();
	}

}
