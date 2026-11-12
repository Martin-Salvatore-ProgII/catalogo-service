package com.example.catalogo.sync.infrastructure.redis.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Valor de catedra:sync:changes:{version} (REF 14.5).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VersionChangesResponse {
	private Long version;
	private Changes changes;

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Changes {
		private List<Long> professionalCategories;
		private List<Long> professionals;
		private List<Long> weeklySchedules;
	}
}
