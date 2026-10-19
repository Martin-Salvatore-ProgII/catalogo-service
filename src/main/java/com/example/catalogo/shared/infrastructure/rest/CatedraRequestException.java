package com.example.catalogo.shared.infrastructure.rest;

/**
 * Un pedido a la cátedra falló. El mensaje es seguro para logs y para el estado de
 * sincronización: dice el tipo de falla, nunca la dirección de la cátedra ni el JWT.
 */
public class CatedraRequestException extends RuntimeException {

	public CatedraRequestException(String message) {
		super(message);
	}

}
