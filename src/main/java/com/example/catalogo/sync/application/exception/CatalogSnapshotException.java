package com.example.catalogo.sync.application.exception;

/**
 * La cátedra no entregó un snapshot completo. El mensaje es seguro para guardarlo como error de
 * sincronización: no muestra la dirección de la cátedra ni el JWT.
 */
public class CatalogSnapshotException extends RuntimeException {

	public CatalogSnapshotException(String message) {
		super(message);
	}

}
