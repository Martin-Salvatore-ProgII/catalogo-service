package com.example.catalogo.sync.domain.model;

// Cómo se aplicó una sincronización: reemplazo completo desde el snapshot (CU-02) o avance por
// versiones desde Redis (CU-01).
public enum SyncType {
	SNAPSHOT,
	INCREMENTAL
}
