package com.example.catalogo.sync.application.exception;

/**
 * No se pudieron leer las versiones o los cambios del catálogo en la cátedra (Redis no responde,
 * rechaza el pedido o devolvió algo que no tiene sentido). El mensaje es seguro para guardarlo
 * como error de sincronización: no muestra la dirección de Redis ni sus credenciales.
 */
public class CatalogChangesException extends RuntimeException {

	public CatalogChangesException(String message) {
		super(message);
	}

}
