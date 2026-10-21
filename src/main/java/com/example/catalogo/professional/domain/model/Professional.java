package com.example.catalogo.professional.domain.model;

import java.time.Instant;

import com.example.catalogo.category.domain.model.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Profesional, con los mismos datos que publica la cátedra (REF §7).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Professional {
	// El id es el que asigna la cátedra, no uno propio.
	private Long id;
	private Long categoryId;
	private String firstName;
	private String lastName;
	private boolean enabled;
	private Instant createdAt;
	private Instant updatedAt;

	// Habilitado efectivo (ADR-0014): solo si él y su categoría lo están. Está en el modelo porque la
	// usan la búsqueda y la agenda, y así la regla queda escrita una sola vez. Sin categoría
	// conocida no se lo ofrece: es preferible ocultarlo a ofrecer algo que la cátedra rechace.
	public boolean isEffectivelyEnabled(Category category) {
		return enabled && category != null && category.isEnabled();
	}
}
