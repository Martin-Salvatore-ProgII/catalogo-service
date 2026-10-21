package com.example.catalogo.sync.domain.ports.in;

// Un ciclo de sincronización del catálogo (CU-01). Lo disparan el arranque, el chequeo periódico
// y, desde la M5, el aviso de Kafka (ADR-0006).
public interface SynchronizeCatalogUseCase {

	void synchronizeCatalog();

}
