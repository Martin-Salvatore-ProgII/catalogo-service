package com.example.catalogo;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Un Redis real (contenedor) que hace de Redis de la cátedra, cargado con el formato de la REF §14.
 * Los datos son los del snapshot real que publicó la cátedra (resources/catedra/snapshot.json);
 * los casos borde se arman modificando lo que ya está cargado (ADR-0044).
 */
public final class FakeRedis {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private FakeRedis() {
	}

	// El usuario de la cátedra de prueba (resources/catedra/integration.json), con la ACL de la REF
	// §14.1: lee catedra:sync:* y solo escribe en su namespace. El usuario por defecto, sin clave,
	// sigue existiendo: es el que usan las pruebas para cargar datos.
	public static GenericContainer<?> newContainer() {
		return new GenericContainer<>(DockerImageName.parse("redis:8"))
				.withExposedPorts(6379)
				.withCommand("redis-server", "--user", "grp_proyecto-de-prueba", "on", ">clave-redis-de-prueba",
						"%R~catedra:sync:*", "~alumnos:proyecto-de-prueba:*", "+@read", "+@write", "+@connection");
	}

	public static StringRedisTemplate templateFor(GenericContainer<?> container) {
		LettuceConnectionFactory factory = new LettuceConnectionFactory(
				new RedisStandaloneConfiguration(container.getHost(), container.getMappedPort(6379)));
		factory.afterPropertiesSet();
		StringRedisTemplate template = new StringRedisTemplate(factory);
		template.afterPropertiesSet();
		return template;
	}

	public static void clear(StringRedisTemplate template) {
		template.execute((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
			connection.serverCommands().flushAll();
			return null;
		});
	}

	// Carga las tres colecciones del snapshot real como hashes (field = id, value = JSON) y publica
	// la versión: current-version, oldest-available-version y changes:{version} con todos los ids.
	public static void publishSnapshotAsVersion(StringRedisTemplate template, long version) {
		JsonNode snapshot = snapshot();
		putEntities(template, "catedra:sync:professional-categories", snapshot.get("professionalCategories"));
		putEntities(template, "catedra:sync:professionals", snapshot.get("professionals"));
		putEntities(template, "catedra:sync:weekly-schedules", snapshot.get("weeklySchedules"));
		publishVersions(template, version, version);
		putChanges(template, version, ids(snapshot.get("professionalCategories")), ids(snapshot.get("professionals")),
				ids(snapshot.get("weeklySchedules")));
	}

	public static void publishVersions(StringRedisTemplate template, long current, long oldest) {
		template.opsForValue().set("catedra:sync:current-version", String.valueOf(current));
		template.opsForValue().set("catedra:sync:oldest-available-version", String.valueOf(oldest));
	}

	public static void putChanges(StringRedisTemplate template, long version, String categoryIds, String professionalIds,
			String scheduleIds) {
		template.opsForValue().set("catedra:sync:changes:" + version, "{\"version\":" + version
				+ ",\"changes\":{\"professionalCategories\":" + categoryIds + ",\"professionals\":" + professionalIds
				+ ",\"weeklySchedules\":" + scheduleIds + "}}");
	}

	private static void putEntities(StringRedisTemplate template, String key, JsonNode entities) {
		for (JsonNode entity : entities) {
			template.opsForHash().put(key, entity.get("id").asString(), entity.toString());
		}
	}

	private static String ids(JsonNode entities) {
		StringBuilder ids = new StringBuilder("[");
		for (JsonNode entity : entities) {
			ids.append(ids.length() > 1 ? "," : "").append(entity.get("id").asString());
		}
		return ids.append("]").toString();
	}

	public static JsonNode snapshot() {
		try (InputStream in = FakeRedis.class.getResourceAsStream("/catedra/snapshot.json")) {
			return JSON.readTree(in);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}
