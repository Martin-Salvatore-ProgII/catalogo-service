package com.example.catalogo.sync.application.usecases;

// Cómo terminó de aplicarse una versión incremental.
enum IncrementalVersionResult {
	// Los cambios quedaron guardados y la versión local avanzó.
	APPLIED,
	// Otro ciclo ya cambió la versión local: no se guardó nada y no es un error (ADR-0007).
	CONCURRENT_CHANGE,
	// Falta algún dato en Redis o no se puede leer: el estado no se puede verificar y corresponde
	// un snapshot (ADR-0009, ENUNCIADO §6.2).
	UNVERIFIABLE
}
