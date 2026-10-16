package com.example.catalogo.sync.infrastructure.persistence.entity;

import java.time.Instant;

import com.example.catalogo.sync.domain.model.SyncType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sync_state")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStateEntity {

	// La tabla tiene una sola fila, con este id. La crea la migración y la base no admite otra.
	public static final int SINGLE_ROW_ID = 1;

	@Id
	private Integer id;
	private Long localVersion;
	private boolean syncInProgress;
	private Instant lastSuccessAt;
	@Enumerated(EnumType.STRING)
	private SyncType lastSuccessType;
	private Instant lastErrorAt;
	private String lastError;
}
