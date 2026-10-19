package com.example.catalogo.sync.infrastructure.rest.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Respuesta de GET /api/synchronization/snapshot (REF §7). generatedAt no se usa.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogSnapshotResponse {
	private Long snapshotVersion;
	private List<CategorySnapshotDto> professionalCategories;
	private List<ProfessionalSnapshotDto> professionals;
	private List<WeeklyScheduleSnapshotDto> weeklySchedules;
}
