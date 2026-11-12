package com.example.catalogo.shared.infrastructure.config;

import com.example.catalogo.shared.infrastructure.rest.NoRetryRequestFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia la API de la cátedra, compartido por todo lo que la llama: el pedido de la
 * integración al arrancar y el snapshot del catálogo.
 */
@Configuration
@EnableConfigurationProperties(CatedraProperties.class)
public class CatedraRestClientConfig {

	// Todo pedido a la cátedra va con el JWT técnico (REF 5.3). Sin timeouts, una cátedra que
	// acepta la conexión y no responde dejaría el pedido esperando para siempre.
	// La conexión no reintenta por su cuenta
	@Bean
	public RestClient catedraRestClient(CatedraProperties properties) {
		return RestClient.builder()
				.baseUrl(properties.apiUrl())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.technicalToken())
				.requestFactory(NoRetryRequestFactory.create(properties.connectTimeout(), properties.readTimeout()))
				.build();
	}

}
