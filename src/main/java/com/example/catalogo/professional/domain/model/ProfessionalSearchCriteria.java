package com.example.catalogo.professional.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

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
	// Habilitado efectivo (ADR-0014): true, solo los habilitados; false, solo los deshabilitados.
	private Boolean enabled;
	// Las categorías habilitadas, para resolver el habilitado efectivo. No las elige el usuario: las
	// completa el caso de uso con el puerto de category (ADR-0064).
	private Set<Long> enabledCategoryIds;
	// Disponibilidad de agenda (ADR-0013): una fecha y, opcionalmente, una franja con sus dos extremos.
	private LocalDate date;
	private LocalTime timeFrom;
	private LocalTime timeTo;
}
