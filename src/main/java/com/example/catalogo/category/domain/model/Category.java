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
@NoArgsConstructor
@AllArgsConstructor
public class Category {
	// El id es el que asigna la cátedra, no uno propio.
	private Long id;
	private String name;
	private String description;
	private boolean enabled;
	private Instant createdAt;
	private Instant updatedAt;
}
