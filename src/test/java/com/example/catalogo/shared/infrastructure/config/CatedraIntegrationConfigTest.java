package com.example.catalogo.shared.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import com.example.catalogo.FakeCatedra;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

// Sin Spring: se arman el cliente y la configuración a mano, como en producción, contra una cátedra
// falsa (WireMock) a la que cada prueba le programa la respuesta. Los timeouts son cortos para que
// las pruebas de demoras no tarden.
class CatedraIntegrationConfigTest {

	private static final String PATH = CatedraIntegrationConfig.INTEGRATION_PATH;
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(1);
	private static final Duration READ_TIMEOUT = Duration.ofMillis(500);
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final GenericContainer<?> catedra = FakeCatedra.newContainer();

	private final CatedraIntegrationConfig config = new CatedraIntegrationConfig();

	@BeforeAll
	static void startCatedra() {
		catedra.start();
	}

	@AfterAll
	static void stopCatedra() {
		catedra.stop();
	}

	@BeforeEach
	void resetCatedra() {
		FakeCatedra.reset(catedra);
	}

	@Test
	void loadsTheIntegrationWhenItIsProvisioned() {
		FakeCatedra.stub(catedra, integrationMapping().toString());

		CatedraIntegration integration = config.catedraIntegration(client());

		assertThat(integration.provisioningStatus()).isEqualTo("PROVISIONED");
		assertThat(integration.redisHost()).isEqualTo("redis.catedra.internal");
		assertThat(integration.redisPort()).isEqualTo(6379);
		assertThat(integration.redisPassword()).isEqualTo("clave-redis-de-prueba");
		assertThat(integration.kafkaCatalogTopic()).isEqualTo("catedra.catalog.proyecto-de-prueba");
	}

	@Test
	void sendsTheTechnicalTokenAsBearer() {
		// La cátedra falsa solo responde si llega el encabezado exacto; si no, contesta 404.
		ObjectNode mapping = integrationMapping();
		((ObjectNode) mapping.get("request")).putObject("headers")
				.putObject("Authorization")
				.put("equalTo", "Bearer " + FakeCatedra.TECHNICAL_TOKEN);
		FakeCatedra.stub(catedra, mapping.toString());

		assertThat(config.catedraIntegration(client()).provisioningStatus()).isEqualTo("PROVISIONED");
	}

	@Test
	void failsWhenTheIntegrationIsNotProvisioned() {
		ObjectNode mapping = integrationMapping();
		((ObjectNode) mapping.get("response").get("jsonBody")).put("provisioningStatus", "PENDING");
		FakeCatedra.stub(catedra, mapping.toString());

		assertThatThrownBy(() -> config.catedraIntegration(client()))
				.isInstanceOf(CatedraIntegrationException.class)
				.hasMessage("Catedra integration is in state PENDING");
	}

	@Test
	void doesNotRetryWhenTheTechnicalTokenIsRejected() {
		FakeCatedra.stub(catedra, respondingWith("\"status\": 401"));

		assertThatThrownBy(() -> config.catedraIntegration(client()))
				.isInstanceOf(CatedraIntegrationException.class)
				.hasMessage("The catedra rejected the technical JWT");
		assertThat(FakeCatedra.countRequests(catedra, "GET", PATH)).isEqualTo(1);
	}

	@Test
	void retriesOnceAndFailsWhenTheCatedraDoesNotAnswerInTime() {
		FakeCatedra.stub(catedra, respondingWith("\"status\": 200, \"fixedDelayMilliseconds\": 1500"));

		assertThatThrownBy(() -> config.catedraIntegration(client()))
				.isInstanceOf(CatedraIntegrationException.class)
				.hasMessageStartingWith("Catedra service unavailable");
		assertThat(FakeCatedra.countRequests(catedra, "GET", PATH)).isEqualTo(2);
	}

	@Test
	void retriesOnceAndFailsWhenTheCatedraAnswersWithAServerError() {
		FakeCatedra.stub(catedra, respondingWith("\"status\": 503"));

		assertThatThrownBy(() -> config.catedraIntegration(client()))
				.isInstanceOf(CatedraIntegrationException.class)
				.hasMessage("Catedra service unavailable (HTTP 503)");
		assertThat(FakeCatedra.countRequests(catedra, "GET", PATH)).isEqualTo(2);
	}

	@Test
	void makesExactlyTwoAttemptsWhenTheConnectionIsAlwaysCut() {
		// Exactamente dos: si el cliente HTTP reintentara por su cuenta, llegarían más pedidos de los
		// que decide el ADR-0029, y sin dejar rastro en el log.
		FakeCatedra.stub(catedra, respondingWith("\"fault\": \"CONNECTION_RESET_BY_PEER\""));

		assertThatThrownBy(() -> config.catedraIntegration(client()))
				.isInstanceOf(CatedraIntegrationException.class)
				.hasMessageStartingWith("Catedra service unavailable");
		assertThat(FakeCatedra.countRequests(catedra, "GET", PATH)).isEqualTo(2);
	}

	@Test
	void recoversWhenOnlyTheFirstAttemptFails() {
		// Escenario de WireMock: el primer pedido corta la conexión y el segundo responde bien.
		FakeCatedra.stub(catedra, """
				{
					"scenarioName": "corte-breve",
					"requiredScenarioState": "Started",
					"newScenarioState": "recuperada",
					"request": { "method": "GET", "url": "%s" },
					"response": { "fault": "CONNECTION_RESET_BY_PEER" }
				}
				""".formatted(PATH));
		ObjectNode recovered = integrationMapping();
		recovered.put("scenarioName", "corte-breve");
		recovered.put("requiredScenarioState", "recuperada");
		FakeCatedra.stub(catedra, recovered.toString());

		assertThat(config.catedraIntegration(client()).provisioningStatus()).isEqualTo("PROVISIONED");
		assertThat(FakeCatedra.countRequests(catedra, "GET", PATH)).isEqualTo(2);
	}

	@Test
	void failsWhenNothingAnswersAtTheCatedraAddressWithoutShowingIt() throws IOException {
		int closedPort;
		try (ServerSocket socket = new ServerSocket(0)) {
			closedPort = socket.getLocalPort();
		}
		String address = "localhost:" + closedPort;

		assertThatThrownBy(() -> config.catedraIntegration(client("http://" + address)))
				.isInstanceOf(CatedraIntegrationException.class)
				.hasMessageStartingWith("Catedra service unavailable")
				.hasMessageNotContaining(address)
				.hasNoCause();
	}

	@Test
	void failsWithoutRetryingOnAnUnexpectedResponse() {
		FakeCatedra.stub(catedra, respondingWith("\"status\": 404"));

		assertThatThrownBy(() -> config.catedraIntegration(client()))
				.isInstanceOf(CatedraIntegrationException.class)
				.hasMessage("Unexpected response from the catedra: HTTP 404");
		assertThat(FakeCatedra.countRequests(catedra, "GET", PATH)).isEqualTo(1);
	}

	@Test
	void doesNotExposeSecretsWhenPrinted() {
		CatedraIntegration integration = new CatedraIntegration("grupo", "redis-host", 6379, "redis-user",
				"redis-secret", "kafka-host:9092", "consumer-group", "catalog-topic", "PROVISIONED");
		CatedraProperties properties = new CatedraProperties("http://catedra-host:1234", "technical-jwt",
				CONNECT_TIMEOUT, READ_TIMEOUT);

		assertThat(integration.toString())
				.doesNotContain("grupo", "redis-host", "redis-user", "redis-secret", "kafka-host", "consumer-group", "catalog-topic");
		assertThat(properties.toString()).doesNotContain("technical-jwt", "catedra-host");
	}

	private RestClient client() {
		return client(FakeCatedra.baseUrl(catedra));
	}

	private RestClient client(String baseUrl) {
		CatedraProperties properties = new CatedraProperties(baseUrl, FakeCatedra.TECHNICAL_TOKEN, CONNECT_TIMEOUT, READ_TIMEOUT);
		return new CatedraRestClientConfig().catedraRestClient(properties);
	}

	// La respuesta PROVISIONED de la REF §5.3, la misma que usa la aplicación completa en las pruebas.
	private static ObjectNode integrationMapping() {
		return (ObjectNode) JSON.readTree(FakeCatedra.readResource(FakeCatedra.INTEGRATION_MAPPING));
	}

	private static String respondingWith(String response) {
		return """
				{ "request": { "method": "GET", "url": "%s" }, "response": { %s } }
				""".formatted(PATH, response);
	}

}
