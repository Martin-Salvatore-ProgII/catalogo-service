package com.example.catalogo.sync.infrastructure.web.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Esquema SyncStatus del contrato. Los campos sin valor se devuelven en null, no se omiten.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusResponse {
	private Long localVersion;
	private boolean syncInProgress;
	private Instant lastSuccessAt;
	private String lastSuccessType;
	private Instant lastErrorAt;
	private String lastError;
}
