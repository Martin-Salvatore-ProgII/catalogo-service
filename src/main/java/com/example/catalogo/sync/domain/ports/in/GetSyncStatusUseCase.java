package com.example.catalogo.sync.domain.ports.in;

import com.example.catalogo.sync.domain.model.SyncState;

public interface GetSyncStatusUseCase {

	SyncState getSyncStatus();

}
