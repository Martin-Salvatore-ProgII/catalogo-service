package com.example.catalogo.professional.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un resultado de la búsqueda (esquema ProfessionalSummary del contrato). El campo enabled, que es
// el habilitado efectivo de ADR-0014, se agrega junto con su filtro.
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
}
