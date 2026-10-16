package com.example.catalogo.sync.domain.ports.out;

import java.time.Instant;

import com.example.catalogo.sync.domain.model.SyncState;
import com.example.catalogo.sync.domain.model.SyncType;

public interface SyncStateRepository {

	// Hay un solo estado de sincronización y siempre existe: no hay "no encontrado".
	SyncState getCurrent();

	void markSyncStarted();

	// La versión solo avanza si sigue siendo la que se leyó al empezar el ciclo (ADR-0007). Si otro
	// ciclo ya la cambió, no modifica nada y devuelve false. Con expectedVersion null se espera
	// que todavía no haya versión local.
	boolean recordSuccessIfVersionIs(Long expectedVersion, long newVersion, SyncType type, Instant succeededAt);

	// Ante una falla la versión no cambia: queda la última aplicada por completo (ADR-0011).
	void recordFailure(String error, Instant failedAt);

	// Un proceso que recién arranca no tiene ningún ciclo en curso: borra la marca que pudo dejar
	// una caída a mitad de una sincronización.
	void clearSyncInProgress();

}
