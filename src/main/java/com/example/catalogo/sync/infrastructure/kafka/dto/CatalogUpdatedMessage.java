package com.example.catalogo.sync.infrastructure.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Aviso CatalogUpdated (REF §15.3). newVersion, occurredAt y source no se usan: la decisión sale
// de comparar versiones con Redis (ADR-0006), no del número del aviso.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogUpdatedMessage {
	private String eventId;
	private String eventType;
	private Long newVersion;
}
