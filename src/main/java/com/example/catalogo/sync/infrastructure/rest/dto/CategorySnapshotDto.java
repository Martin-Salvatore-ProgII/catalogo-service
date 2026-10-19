package com.example.catalogo.sync.infrastructure.rest.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategorySnapshotDto {
	private Long id;
	private String name;
	private String description;
	private boolean enabled;
	private Instant createdAt;
	private Instant updatedAt;
}
