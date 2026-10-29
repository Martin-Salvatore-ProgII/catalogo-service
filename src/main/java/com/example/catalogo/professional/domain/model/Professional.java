package com.example.catalogo.professional.domain.model;

import java.time.Instant;

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
}
