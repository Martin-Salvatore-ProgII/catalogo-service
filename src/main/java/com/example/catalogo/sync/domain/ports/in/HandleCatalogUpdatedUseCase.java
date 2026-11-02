package com.example.catalogo.sync.domain.ports.in;

public interface HandleCatalogUpdatedUseCase {

	// Procesa un aviso CatalogUpdated (CU-01). eventId es null si el aviso no se pudo leer: igual
	// se sincroniza, porque la decisión no depende de su contenido (CU-01, 1b).
	void handleCatalogUpdated(String eventId);

}
