package com.example.catalogo.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.example.catalogo.FakeCatedra;
import com.example.catalogo.TestCatedraConfiguration;
import com.example.catalogo.TestJwtKeysConfiguration;
import com.example.catalogo.TestcontainersConfiguration;
import com.example.catalogo.sync.application.service.SyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Inicialización desde el snapshot de punta a punta (CU-02, ENUNCIADO §11): la aplicación completa,
 * PostgreSQL real, la transacción real y la cátedra falsa respondiendo el snapshot real.
 *
 * <p>El intervalo largo del chequeo periódico evita que corra durante la prueba, y le da a esta
 * clase su propio contexto: arranca con una base nueva, en la que la única sincronización es la
 * del arranque.
 */
@SpringBootTest(properties = "catalogo.sync.check-interval=1h")
@Import({ TestcontainersConfiguration.class, TestJwtKeysConfiguration.class, TestCatedraConfiguration.class })
class CatalogSnapshotSyncTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private SyncService syncService;

	@Autowired
	private GenericContainer<?> catedraContainer;

	@Test
	void loadsTheSnapshotAtStartupWhenTheDatabaseIsEmpty() {
		assertThat(count("professional_category")).isEqualTo(8);
		assertThat(count("professional")).isEqualTo(24);
		assertThat(count("weekly_schedule")).isEqualTo(44);
		// Las deshabilitadas también se guardan (REF §7).
		assertThat(jdbcClient.sql("SELECT count(*) FROM professional WHERE NOT enabled").query(Integer.class).single())
				.isEqualTo(3);

		Map<String, Object> state = syncState();
		assertThat(state).containsEntry("local_version", 1L)
				.containsEntry("last_success_type", "SNAPSHOT")
				.containsEntry("sync_in_progress", false);
		assertThat(state.get("last_success_at")).isNotNull();
		assertThat(state.get("last_error")).isNull();
	}

	@Test
	void doesNotAskForTheSnapshotAgainWhenThereIsALocalVersion() {
		int requestsBefore = FakeCatedra.countRequests(catedraContainer, "GET", FakeCatedra.SNAPSHOT_PATH);

		syncService.synchronizeCatalog();

		assertThat(FakeCatedra.countRequests(catedraContainer, "GET", FakeCatedra.SNAPSHOT_PATH)).isEqualTo(requestsBefore);
	}

	// Cambia la cátedra falsa y el estado: se descarta el contexto al terminar.
	@Test
	@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
	void keepsTheCurrentCopyWhenTheSnapshotHasAnInconsistentReference() {
		// Un profesional que apunta a una categoría que no está en el snapshot (CU-02, 2a).
		ObjectNode snapshot = (ObjectNode) JSON.readTree(FakeCatedra.readResource(FakeCatedra.SNAPSHOT));
		((ObjectNode) snapshot.get("professionals").get(0)).put("categoryId", 999);
		FakeCatedra.stub(catedraContainer, FakeCatedra.snapshotMapping(snapshot.toString()));
		jdbcClient.sql("UPDATE sync_state SET local_version = NULL").update();

		syncService.synchronizeCatalog();

		// La transacción se deshizo entera, borrados incluidos: la copia anterior sigue completa.
		assertThat(count("professional_category")).isEqualTo(8);
		assertThat(count("professional")).isEqualTo(24);
		assertThat(count("weekly_schedule")).isEqualTo(44);
		Map<String, Object> state = syncState();
		assertThat(state.get("local_version")).isNull();
		assertThat(state).containsEntry("sync_in_progress", false);
		assertThat(state).containsEntry("last_error",
				"Internal error: catalog snapshot could not be stored (DataIntegrityViolationException)");
		assertThat(state.get("last_error_at")).isNotNull();
	}

	private int count(String table) {
		return jdbcClient.sql("SELECT count(*) FROM " + table).query(Integer.class).single();
	}

	private Map<String, Object> syncState() {
		return jdbcClient.sql("SELECT * FROM sync_state").query().singleRow();
	}

}
