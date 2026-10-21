package com.example.catalogo.sync.application.exception;

/**
 * El catálogo todavía no tiene una versión local: no hay copia con la que responder. Se traduce a
 * 503 {@code CATALOG_NOT_READY}.
 */
public class CatalogNotReadyException extends RuntimeException {

	public CatalogNotReadyException() {
		super("The catalog has no local version yet");
	}

}
