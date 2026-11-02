package com.example.catalogo.sync.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Las dos versiones que publica la cátedra (REF §14.2).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogVersions {
	// Última versión publicada (C).
	private long current;
	// Versión más vieja desde la que se puede avanzar versión por versión (O).
	private long oldestAvailable;
}
