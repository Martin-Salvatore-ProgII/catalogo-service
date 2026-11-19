package com.example.catalogo.category.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Categoría de profesionales, con los mismos datos que publica la cátedra (REF §7).
@Getter
@Setter
@Builder
@NoArgsConstructor  // Jackson o Hibernate no saben que datos vendran a un Objeto, por ello inicializan con constructor vacio y luego asignan valores
@AllArgsConstructor
public class Category {
	// El valor del id es el que asigna la cátedra, no uno propio.
	private Long id;
	private String name;
	private String description;
	private boolean enabled;
	private Instant createdAt;   //Time se repite una vez todos los dias mientras que Instant es unico en la vida
	private Instant updatedAt;
}
