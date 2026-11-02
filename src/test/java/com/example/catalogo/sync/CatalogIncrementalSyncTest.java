package com.example.catalogo.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import com.example.catalogo.CatalogTables;
import com.example.catalogo.FakeCatedra;
import com.example.catalogo.FakeKafka;
import com.example.catalogo.FakeRedis;
import com.example.catalogo.TestCatedraConfiguration;
import com.example.catalogo.TestJwtKeysConfiguration;
import com.example.catalogo.TestcontainersConfiguration;
import com.example.catalogo.sync.application.service.SyncService;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * La sincronización de punta a punta, con todo real salvo la API de la cátedra, que es WireMock
 * (ADR-0044): PostgreSQL, el Redis de la cátedra (con la ACL de solo lectura), el Kafka de la
 * cátedra y el servicio completo. Cada escenario es una evidencia del ENUNCIADO §11.
 *
 * <p>Parte siempre del mismo estado: la cátedra publicó solo la versión 1 (como la real) y el
 * servicio la tiene aplicada. El intervalo largo del chequeo periódico evita que corra solo; los
 * escenarios que lo necesitan lo disparan a mano con {@code synchronizeCatalog()}, que es lo que
 * hace el scheduler.
 */
// Cambia Redis, Kafka y la base: se descarta el contexto al terminar, para que ninguna otra clase que
// comparta su configuración parta de un estado modificado.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = "catalogo.sync.check-interval=1h")
@Import({ TestcontainersConfiguration.class, TestJwtKeysConfiguration.class, TestCatedraConfiguration.class })
class CatalogIncrementalSyncTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final String TOPIC = "catedra.catalog.proyecto-de-prueba";
	private static final String GROUP = "alumnos-proyecto-de-prueba";

	@Autowired
	private JdbcClient jdbcClient;
	@Autowired
	private SyncService syncService;
	@Autowired
	private GenericContainer<?> catedraContainer;
	@Autowired
	private GenericContainer<?> redisContainer;
	@Autowired
	private KafkaListenerEndpointRegistry registry;

	private StringRedisTemplate redis;
	private int snapshotRequestsAtStart;

	@BeforeEach
	void startFromVersionOne() {
		redis = FakeRedis.templateFor(redisContainer);
		FakeCatedra.reset(catedraContainer);
		FakeRedis.clear(redis);
		FakeRedis.publishSnapshotAsVersion(redis, 1);
		jdbcClient.sql("DELETE FROM processed_event").update();
		CatalogTables.clear(jdbcClient);
		CatalogTables.resetSyncState(jdbcClient);
		syncService.synchronizeCatalog();
		assertThat(syncState()).containsEntry("local_version", 1L).containsEntry("last_success_type", "SNAPSHOT");
		snapshotRequestsAtStart = snapshotRequests();
		await().atMost(Duration.ofSeconds(30)).until(() -> registry.getListenerContainers().stream()
				.anyMatch(container -> !container.getAssignedPartitions().isEmpty()));
	}

	@Test
	void appliesANewVersionIncrementallyWhenTheNoticeArrives() {
		publishVersionTwo();

		FakeKafka.publish(TOPIC, "2", notice("catalog-event-" + UUID.randomUUID(), 2));

		await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
				assertThat(syncState()).containsEntry("local_version", 2L).containsEntry("last_success_type", "INCREMENTAL"));
		// Lo que cambió en la versión 2 está en la copia: un profesional dado de baja, una categoría
		// nueva y un profesional y un horario nuevos que la usan.
		assertThat(jdbcClient.sql("SELECT enabled FROM professional WHERE id = 201").query(Boolean.class).single()).isFalse();
		assertThat(jdbcClient.sql("SELECT last_name FROM professional WHERE id = 201").query(String.class).single())
				.isEqualTo("Actualizado");
		assertThat(jdbcClient.sql("SELECT name FROM professional_category WHERE id = 109").query(String.class).single())
				.isEqualTo("Nueva especialidad");
		assertThat(jdbcClient.sql("SELECT category_id FROM professional WHERE id = 225").query(Long.class).single()).isEqualTo(109L);
		assertThat(count("weekly_schedule WHERE professional_id = 225")).isEqualTo(1);
		// Las entidades que no cambiaron siguen ahí, y no hizo falta un snapshot.
		assertThat(count("professional")).isEqualTo(25);
		assertThat(snapshotRequests()).isEqualTo(snapshotRequestsAtStart);
		assertThat(syncState().get("last_error")).isNull();
	}

	@Test
	void ignoresADuplicatedNoticeAndTheNextCheckStillCatchesUp() {
		publishVersionTwo();
		String eventId = "catalog-event-" + UUID.randomUUID();
		FakeKafka.publish(TOPIC, "2", notice(eventId, 2));
		await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(syncState()).containsEntry("local_version", 2L));

		// La cátedra publica la 3 y llega, repetido, el aviso de la 2 (entrega al menos una vez).
		publishVersionThree();
		RecordMetadata duplicate = FakeKafka.publish(TOPIC, "2", notice(eventId, 2));
		await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
				assertThat(FakeKafka.committedOffset(GROUP, TOPIC, duplicate.partition())).isGreaterThan(duplicate.offset()));

		// El duplicado se reconoció por su eventId: no sincronizó, y quedó un solo registro.
		assertThat(syncState()).containsEntry("local_version", 2L);
		assertThat(count("processed_event WHERE event_id = '" + eventId + "'")).isEqualTo(1);

		// El aviso de la 3 se "perdió": el chequeo periódico compara con Redis y se pone al día.
		syncService.synchronizeCatalog();
		assertThat(syncState()).containsEntry("local_version", 3L).containsEntry("last_success_type", "INCREMENTAL");
	}

	@Test
	void catchesUpWithoutAnyNoticeWhenThePeriodicCheckRuns() {
		publishVersionTwo();

		syncService.synchronizeCatalog();

		assertThat(syncState()).containsEntry("local_version", 2L).containsEntry("last_success_type", "INCREMENTAL");
		assertThat(snapshotRequests()).isEqualTo(snapshotRequestsAtStart);
	}

	@Test
	void rebuildsFromTheSnapshotWhenTheServiceFellBehindTheAvailableHistory() {
		// El servicio estuvo apagado: la cátedra ya está en la 9 y solo conserva desde la 5. La local
		// es la 1 (REF §18.2).
		FakeRedis.publishVersions(redis, 9, 5);
		stubSnapshot(9, "Reconstruido");

		FakeKafka.publish(TOPIC, "9", notice("catalog-event-" + UUID.randomUUID(), 9));

		await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
				assertThat(syncState()).containsEntry("local_version", 9L).containsEntry("last_success_type", "SNAPSHOT"));
		assertThat(snapshotRequests()).isEqualTo(snapshotRequestsAtStart + 1);
		assertThat(jdbcClient.sql("SELECT last_name FROM professional WHERE id = 201").query(String.class).single())
				.isEqualTo("Reconstruido");
	}

	@Test
	void rebuildsFromTheSnapshotWhenTheHistoryOfAVersionIsMissing() {
		// Discontinuidad: la cátedra dice estar en la 2 pero no tiene sus cambios.
		FakeRedis.publishVersions(redis, 2, 1);
		stubSnapshot(2, "Reconstruido");

		syncService.synchronizeCatalog();

		assertThat(syncState()).containsEntry("local_version", 2L).containsEntry("last_success_type", "SNAPSHOT");
		assertThat(snapshotRequests()).isEqualTo(snapshotRequestsAtStart + 1);
	}

	@Test
	void keepsTheCopyAndRecordsTheErrorWhenAnEntityOfTheVersionIsNotInRedis() {
		// La versión 2 lista un profesional que no está en su hash: no se puede verificar, hay un
		// snapshot. Con el snapshot también caído, la copia queda intacta y el error se registra.
		FakeRedis.publishVersions(redis, 2, 1);
		FakeRedis.putChanges(redis, 2, "[]", "[999]", "[]");
		FakeCatedra.stub(catedraContainer, "{\"request\":{\"method\":\"GET\",\"url\":\"" + FakeCatedra.SNAPSHOT_PATH
				+ "\"},\"response\":{\"status\":503}}");

		syncService.synchronizeCatalog();

		Map<String, Object> state = syncState();
		assertThat(state).containsEntry("local_version", 1L).containsEntry("sync_in_progress", false);
		assertThat((String) state.get("last_error")).startsWith("Catedra service unavailable");
		assertThat(count("professional")).isEqualTo(24);
	}

	@Test
	void recordsTheErrorAndKeepsTheCopyWhenRedisHasNoVersions() {
		redis.delete("catedra:sync:current-version");

		syncService.synchronizeCatalog();

		assertThat(syncState()).containsEntry("local_version", 1L);
		assertThat((String) syncState().get("last_error")).contains("catedra:sync:current-version");
		assertThat(snapshotRequests()).isEqualTo(snapshotRequestsAtStart);
	}

	// Versión 2: el profesional 201 cambia de apellido y se da de baja, aparece la categoría 109 y,
	// con ella, el profesional 225 y su horario 345. Como publica la cátedra: estado actual en los
	// hashes, ids afectados en changes:2 y las versiones.
	private void publishVersionTwo() {
		putEntity("professionals", 201, node -> node.put("lastName", "Actualizado").put("enabled", false));
		redis.opsForHash().put("catedra:sync:professional-categories", "109", newCategory(109).toString());
		redis.opsForHash().put("catedra:sync:professionals", "225", newProfessional(225, 109).toString());
		redis.opsForHash().put("catedra:sync:weekly-schedules", "345", newSchedule(345, 225).toString());
		FakeRedis.putChanges(redis, 2, "[109]", "[201,225]", "[345]");
		FakeRedis.publishVersions(redis, 2, 1);
	}

	private void publishVersionThree() {
		putEntity("professionals", 202, node -> node.put("firstName", "Tercera"));
		FakeRedis.putChanges(redis, 3, "[]", "[202]", "[]");
		FakeRedis.publishVersions(redis, 3, 1);
	}

	private void putEntity(String hash, long id, java.util.function.Consumer<ObjectNode> change) {
		String key = "catedra:sync:" + hash;
		ObjectNode entity = (ObjectNode) JSON.readTree((String) redis.opsForHash().get(key, String.valueOf(id)));
		change.accept(entity);
		redis.opsForHash().put(key, String.valueOf(id), entity.toString());
	}

	// El snapshot de la cátedra falsa pasa a ser la versión dada, con el apellido del profesional 201 cambiado.
	private void stubSnapshot(long version, String lastNameOf201) {
		ObjectNode snapshot = (ObjectNode) JSON.readTree(FakeCatedra.readResource(FakeCatedra.SNAPSHOT));
		snapshot.put("snapshotVersion", version);
		for (JsonNode professional : snapshot.get("professionals")) {
			if (professional.get("id").asLong() == 201) {
				((ObjectNode) professional).put("lastName", lastNameOf201);
			}
		}
		FakeCatedra.stub(catedraContainer, FakeCatedra.snapshotMapping(snapshot.toString()));
	}

	private static ObjectNode newCategory(long id) {
		return JSON.createObjectNode().put("id", id).put("name", "Nueva especialidad").put("description", "Creada en la versión 2")
				.put("enabled", true).put("createdAt", "2026-11-01T12:00:00Z").put("updatedAt", "2026-11-01T12:00:00Z");
	}

	private static ObjectNode newProfessional(long id, long categoryId) {
		return JSON.createObjectNode().put("id", id).put("categoryId", categoryId).put("firstName", "Nuevo")
				.put("lastName", "Profesional").put("enabled", true).put("createdAt", "2026-11-01T12:00:00Z")
				.put("updatedAt", "2026-11-01T12:00:00Z");
	}

	private static ObjectNode newSchedule(long id, long professionalId) {
		return JSON.createObjectNode().put("id", id).put("professionalId", professionalId).put("dayOfWeek", "MONDAY")
				.put("startTime", "09:00:00").put("endTime", "12:00:00").put("slotDurationMinutes", 30).put("enabled", true)
				.put("createdAt", "2026-11-01T12:00:00Z").put("updatedAt", "2026-11-01T12:00:00Z");
	}

	private static String notice(String eventId, long version) {
		return "{\"eventId\":\"" + eventId + "\",\"eventType\":\"CatalogUpdated\",\"newVersion\":" + version
				+ ",\"occurredAt\":\"2026-11-01T12:00:00Z\",\"source\":\"MANUAL\",\"schemaVersion\":1}";
	}

	private int snapshotRequests() {
		return FakeCatedra.countRequests(catedraContainer, "GET", FakeCatedra.SNAPSHOT_PATH);
	}

	private int count(String tableAndFilter) {
		return jdbcClient.sql("SELECT count(*) FROM " + tableAndFilter).query(Integer.class).single();
	}

	private Map<String, Object> syncState() {
		return jdbcClient.sql("SELECT * FROM sync_state").query().singleRow();
	}

}
