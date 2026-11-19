package com.example.catalogo.shared.infrastructure.rest;

import java.time.Duration;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * Fábrica de la conexión HTTP del cliente REST de la cátedra, con los timeouts configurados y sin
 * reintentos propios.
 */
public final class NoRetryRequestFactory {

	private NoRetryRequestFactory() {
	}

	// Los únicos reintentos son los del ADR-0029, que dejan rastro en el log. El cliente de
	// java.net.http repite por su cuenta un GET cuyo servidor corta la conexión (con HTTP/1.1, que es
	// lo que habla la cátedra real) y no hay forma de apagarlo en un solo cliente
	// Elegi el de Apache porque si puede deshabilitarse: disableAutomaticRetries().
	public static ClientHttpRequestFactory create(Duration connectTimeout, Duration readTimeout) {
		ConnectionConfig connectionConfig = ConnectionConfig.custom()
				.setConnectTimeout(Timeout.of(connectTimeout))   // definidos en application.yaml
				.setSocketTimeout(Timeout.of(readTimeout))
				.build();
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
				.setDefaultConnectionConfig(connectionConfig)
				.build();
		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.disableAutomaticRetries()
				.build();
		return new HttpComponentsClientHttpRequestFactory(httpClient);
	}

}
// se arma el 