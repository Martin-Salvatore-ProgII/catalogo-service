package com.example.catalogo.sync.infrastructure.catedra.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// lo usa CatalogSnapshotResponse para armar las listas de cada cosa que compone al snapshot
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatedraCategoryDto {
	private Long id;
	private String name;
	private String description;
	private boolean enabled;
	private Instant createdAt;
	private Instant updatedAt;
}
