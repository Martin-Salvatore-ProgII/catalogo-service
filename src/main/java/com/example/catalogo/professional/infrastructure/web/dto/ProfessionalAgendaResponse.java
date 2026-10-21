package com.example.catalogo.professional.infrastructure.web.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// La agenda vigente de un profesional (esquema ProfessionalAgenda del contrato, ADR-0017).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalAgendaResponse {
	private Long id;
	private String firstName;
	private String lastName;
	private CategoryRefResponse category;
	// Habilitado efectivo (ADR-0014): el profesional y su categoría.
	private boolean enabled;
	// Solo los habilitados, de lunes a domingo y por hora de inicio.
	private List<WeeklyScheduleResponse> weeklySchedules;
	private long catalogVersion;
}
