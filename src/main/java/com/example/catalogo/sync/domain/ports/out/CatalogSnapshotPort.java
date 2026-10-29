package com.example.catalogo.sync.domain.ports.out;

import com.example.catalogo.sync.domain.model.CatalogSnapshot;

public interface CatalogSnapshotPort {

	// Pide el catálogo completo a la cátedra. Si no llega completo, el adaptador lanza
	// CatalogSnapshotException con un mensaje que se puede guardar como error de sincronización.
	CatalogSnapshot getSnapshot();

}
