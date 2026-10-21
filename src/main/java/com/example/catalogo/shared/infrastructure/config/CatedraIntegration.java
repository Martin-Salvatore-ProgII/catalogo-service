package com.example.catalogo.shared.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Configuración de la integración que asigna la cátedra (REF §5.3). Solo lleva los campos que usa
 * el catálogo: Redis, Kafka y el topic de avisos del catálogo. Los topics de turnos se ignoran.
 *
 * <p>Vive en memoria: no se guarda en la base ni en archivos (ADR-0005).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CatedraIntegration(
		String groupId,
		String redisHost,
		int redisPort,
		String redisUsername,
		String redisPassword,
		String kafkaBootstrapServers,
		String kafkaConsumerGroupId,
		String kafkaCatalogTopic,
		String provisioningStatus) {

	// Ningún valor de la integración puede aparecer en un log (RNF-02). Un record los imprimiría
	// todos en toString(), incluida la contraseña de Redis; solo se deja ver el estado.
	@Override
	public String toString() {
		return "CatedraIntegration[provisioningStatus=" + provisioningStatus + "]";
	}

}
