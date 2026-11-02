package com.example.catalogo.sync.domain.ports.out;

import com.example.catalogo.sync.domain.model.CatalogVersions;

public interface CatalogVersionPort {

	// Lee las versiones publicadas por la cátedra. Si no responde, o no las publica, el adaptador
	// lanza CatalogChangesException con un mensaje que se puede guardar como error de sincronización.
	CatalogVersions getVersions();

}
