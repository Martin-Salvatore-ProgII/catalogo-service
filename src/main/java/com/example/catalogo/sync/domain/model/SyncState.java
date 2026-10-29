package com.example.catalogo.sync.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Estado de la sincronización del catálogo (ADR-0011).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncState {
	// Última versión del catálogo aplicada por completo. Null: todavía no hay copia local.
	private Long localVersion;
	private boolean syncInProgress;
	private Instant lastSuccessAt;
	private SyncType lastSuccessType;
	private Instant lastErrorAt;
	private String lastError;
}
