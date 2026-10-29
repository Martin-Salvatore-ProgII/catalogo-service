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
public class ProfessionalSnapshotDto {
	private Long id;
	private Long categoryId;
	private String firstName;
	private String lastName;
	private boolean enabled;
	private Instant createdAt;
	private Instant updatedAt;
}
