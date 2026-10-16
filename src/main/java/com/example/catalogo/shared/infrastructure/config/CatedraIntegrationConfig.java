package com.example.catalogo.shared.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Obtiene al arrancar la configuración de la integración con la cátedra (ADR-0005, ENUNCIADO §5
 * punto 3). Si no se puede obtener o no está {@code PROVISIONED}, el servicio no arranca.
 *
 * <p>Está en {@code shared} y no en una feature porque solo la consume infraestructura (la
 * configuración de Redis y Kafka); ningún caso de uso la necesita.
 */
@Slf4j
@Configuration
public class CatedraIntegrationConfig {

	static final String INTEGRATION_PATH = "/api/student/integration";

	private static final String PROVISIONED = "PROVISIONED";

	// Es una lectura: un intento más si la cátedra no responde (ADR-0029). Un corte breve no
	// alcanza para dar la cátedra por caída.
	private static final int MAX_ATTEMPTS = 2;

	// Se obtiene al crear el bean, antes de que Spring arme lo que depende de estos datos. Si
	// falla, Spring corta el arranque con el mensaje de la excepción.
	@Bean
	public CatedraIntegration catedraIntegration(RestClient catedraRestClient) {
		CatedraIntegration integration = fetchWithRetry(catedraRestClient);
		if (!PROVISIONED.equals(integration.provisioningStatus())) {
			throw new CatedraIntegrationException("Catedra integration is in state " + integration.provisioningStatus());
		}
		log.info("Catedra integration obtained, in state {}", PROVISIONED);
		return integration;
	}

	private CatedraIntegration fetchWithRetry(RestClient catedraRestClient) {
		for (int attempt = 1; ; attempt++) {
			try {
				return fetch(catedraRestClient);
			} catch (ResourceAccessException | HttpServerErrorException e) {
				// Sin conexión, timeout o error del lado de la cátedra. Se informa solo el tipo de
				// falla: el mensaje original incluye la URL de la cátedra, que no va a los logs.
				String reason = e instanceof HttpServerErrorException serverError
						? "HTTP " + serverError.getStatusCode().value()
						: e.getMostSpecificCause().getClass().getSimpleName();
				if (attempt == MAX_ATTEMPTS) {
					throw new CatedraIntegrationException("Catedra service unavailable (" + reason + ")");
				}
				log.warn("Catedra service unavailable ({}), attempt {} of {}", reason, attempt, MAX_ATTEMPTS);
			}
		}
	}

	// Un 401 o 403 no se reintenta: repetir el pedido con el mismo JWT da la misma respuesta.
	private CatedraIntegration fetch(RestClient catedraRestClient) {
		try {
			CatedraIntegration integration = catedraRestClient.get()
					.uri(INTEGRATION_PATH)
					.retrieve()
					.body(CatedraIntegration.class);
			if (integration == null) {
				throw new CatedraIntegrationException("Unexpected response from the catedra: empty body");
			}
			return integration;
		} catch (HttpClientErrorException e) {
			if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED) || e.getStatusCode().isSameCodeAs(HttpStatus.FORBIDDEN)) {
				throw new CatedraIntegrationException("The catedra rejected the technical JWT");
			}
			throw new CatedraIntegrationException("Unexpected response from the catedra: HTTP " + e.getStatusCode().value());
		} catch (ResourceAccessException | HttpServerErrorException e) {
			throw e;
		} catch (RestClientException e) {
			throw new CatedraIntegrationException("Unexpected response from the catedra: unreadable body");
		}
	}

}
