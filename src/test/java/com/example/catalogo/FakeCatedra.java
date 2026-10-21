package com.example.catalogo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * La API de la cátedra simulada con WireMock, en un contenedor (ADR-0044, {@code docs/arq/pruebas.md}).
 * Las respuestas se programan con la API de administración de WireMock, por HTTP, así que no hace
 * falta la librería de WireMock en el proyecto.
 */
public final class FakeCatedra {

	public static final String TECHNICAL_TOKEN = "test-technical-token";

	/** Respuesta de {@code GET /api/student/integration} en estado PROVISIONED, con el formato de la REF §5.3. */
	public static final String INTEGRATION_MAPPING = "catedra/integration.json";

	/** Copia de un snapshot real de la cátedra (REF §7): versión 1, 8 categorías, 24 profesionales, 44 horarios. */
	public static final String SNAPSHOT = "catedra/snapshot.json";

	public static final String SNAPSHOT_PATH = "/api/synchronization/snapshot";

	private static final DockerImageName IMAGE = DockerImageName.parse("wiremock/wiremock:3.13.2");
	private static final int PORT = 8080;
	private static final String MAPPINGS_DIR = "/home/wiremock/mappings/";

	private static final HttpClient HTTP = HttpClient.newHttpClient();
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private FakeCatedra() {
	}

	public static GenericContainer<?> newContainer() {
		return new GenericContainer<>(IMAGE)
				.withExposedPorts(PORT)
				.waitingFor(Wait.forHttp("/__admin/health").forStatusCode(200));
	}

	/**
	 * Contenedor que ya arranca respondiendo como la cátedra real: la integración en estado
	 * PROVISIONED y el snapshot real.
	 */
	public static GenericContainer<?> newContainerLikeTheCatedra() {
		return newContainer()
				.withCopyFileToContainer(MountableFile.forClasspathResource(INTEGRATION_MAPPING), MAPPINGS_DIR + "integration.json")
				.withCopyToContainer(Transferable.of(snapshotMapping(readResource(SNAPSHOT))), MAPPINGS_DIR + "snapshot.json");
	}

	/** Respuesta 200 de {@code GET /api/synchronization/snapshot} con el cuerpo dado. */
	public static String snapshotMapping(String snapshotJson) {
		ObjectNode mapping = JSON.createObjectNode();
		mapping.putObject("request").put("method", "GET").put("url", SNAPSHOT_PATH);
		ObjectNode response = mapping.putObject("response").put("status", 200);
		response.putObject("headers").put("Content-Type", "application/json");
		response.set("jsonBody", JSON.readTree(snapshotJson));
		return mapping.toString();
	}

	public static String baseUrl(GenericContainer<?> container) {
		return "http://" + container.getHost() + ":" + container.getMappedPort(PORT);
	}

	public static void stub(GenericContainer<?> container, String mappingJson) {
		admin(container, "POST", "/__admin/mappings", mappingJson);
	}

	public static void reset(GenericContainer<?> container) {
		admin(container, "POST", "/__admin/reset", "");
	}

	public static int countRequests(GenericContainer<?> container, String method, String path) {
		String body = admin(container, "POST", "/__admin/requests/count",
				"{\"method\":\"" + method + "\",\"url\":\"" + path + "\"}");
		return JSON.readTree(body).get("count").asInt();
	}

	public static String readResource(String name) {
		try (var in = FakeCatedra.class.getClassLoader().getResourceAsStream(name)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String admin(GenericContainer<?> container, String method, String path, String body) {
		HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl(container) + path))
				.method(method, HttpRequest.BodyPublishers.ofString(body))
				.header("Content-Type", "application/json")
				.build();
		try {
			HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 300) {
				throw new IllegalStateException("WireMock respondió " + response.statusCode() + ": " + response.body());
			}
			return response.body();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

}
