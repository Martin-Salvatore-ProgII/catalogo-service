package com.example.catalogo.professional.domain.model;

import com.example.catalogo.category.domain.model.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un profesional como resultado de la búsqueda, con su categoría. La categoría es el modelo de
// dominio de la feature category, que se puede usar desde otra feature (ADR-0062).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalSummary {
	private Long id;
	private String firstName;
	private String lastName;
	private Category category;
	// Habilitado efectivo (ADR-0014). En false, la app lo muestra como no disponible para reservar.
	private boolean enabled;
}
