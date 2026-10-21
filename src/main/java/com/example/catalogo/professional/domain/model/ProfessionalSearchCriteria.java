package com.example.catalogo.professional.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Filtros de la búsqueda de profesionales (HU-05). Todos opcionales: un filtro en null no restringe.
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalSearchCriteria {
	private Long categoryId;
	// Texto que tiene que aparecer en el nombre completo, sin distinguir mayúsculas ni tildes.
	private String name;
}
