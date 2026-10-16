package com.example.catalogo.shared.infrastructure.config;

/**
 * La integración con la cátedra no se pudo obtener o no está lista. Corta el arranque del
 * servicio (ADR-0005).
 */
public class CatedraIntegrationException extends RuntimeException {

	public CatedraIntegrationException(String message) {
		super(message);
	}

}
