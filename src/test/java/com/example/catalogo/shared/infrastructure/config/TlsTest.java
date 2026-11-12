package com.example.catalogo.shared.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import javax.net.ssl.SSLHandshakeException;

import com.example.catalogo.TestCatedraConfiguration;
import com.example.catalogo.TestJwtKeysConfiguration;
import com.example.catalogo.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * HTTPS (ADR-0035): el servicio levantado de verdad, en un puerto real, con el perfil tls y el certificado
 * de prueba de src/test/resources/tls (válido solo para localhost). Un cliente que confía en el
 * certificado llega a la API; uno que no, no; y un pedido en texto plano no llega. Las demás pruebas
 * corren sin el perfil, en HTTP. El cliente de java.net.http alcanza acá: su reintento oculto no importa
 * para lo que se prueba (ver NoHiddenRetryTest).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"TLS_CERTIFICATE_LOCATION=classpath:tls/test.crt",
		"TLS_PRIVATE_KEY_LOCATION=classpath:tls/test.key",
		// Solo para la prueba: el cliente que confía en el certificado, como lo hará turnos o la app.
		"spring.ssl.bundle.pem.test-client.truststore.certificate=classpath:tls/test.crt" })
@ActiveProfiles("tls")
@Import({ TestcontainersConfiguration.class, TestJwtKeysConfiguration.class, TestCatedraConfiguration.class })
class TlsTest {

	@LocalServerPort
	private int port;

	@Autowired
	private SslBundles sslBundles;

	// El 401 es la respuesta de la API a un pedido sin token: la conexión cifrada funcionó y el pedido
	// llegó a la seguridad.
	@Test
	void aClientThatTrustsTheCertificateReachesTheApi() throws Exception {
		HttpClient client = HttpClient.newBuilder()
				.sslContext(sslBundles.getBundle("test-client").createSslContext())
				.build();

		HttpResponse<Void> response = client.send(request("https"), HttpResponse.BodyHandlers.discarding());

		assertThat(response.statusCode()).isEqualTo(401);
	}

	@Test
	void aClientThatDoesNotTrustTheCertificateIsRejected() {
		HttpClient client = HttpClient.newHttpClient();

		assertThatThrownBy(() -> client.send(request("https"), HttpResponse.BodyHandlers.discarding()))
				.isInstanceOf(SSLHandshakeException.class);
	}

	// Sin puerto HTTP: Tomcat responde 400 a un pedido en texto plano y no lo pasa a la API.
	@Test
	void plainHttpIsNotServed() throws Exception {
		HttpResponse<Void> response = HttpClient.newHttpClient()
				.send(request("http"), HttpResponse.BodyHandlers.discarding());

		assertThat(response.statusCode()).isEqualTo(400);
	}

	private HttpRequest request(String scheme) {
		return HttpRequest.newBuilder(URI.create(scheme + "://localhost:" + port + "/api/professional-categories"))
				.timeout(Duration.ofSeconds(5))
				.build();
	}

}
