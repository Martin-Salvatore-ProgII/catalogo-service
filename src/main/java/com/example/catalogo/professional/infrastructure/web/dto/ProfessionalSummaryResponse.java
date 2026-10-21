package com.example.catalogo.professional.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un resultado de la búsqueda (esquema ProfessionalSummary del contrato).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalSummaryResponse {
	private Long id;
	private String firstName;
	private String lastName;
	private CategoryRefResponse category;
	// Habilitado efectivo (ADR-0014): el profesional y su categoría.
	private boolean enabled;
}
