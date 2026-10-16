package com.example.catalogo.sync.infrastructure.rest.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

import com.example.catalogo.FakeCatedra;
import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.shared.infrastructure.config.CatedraProperties;
import com.example.catalogo.shared.infrastructure.config.CatedraRestClientConfig;
import com.example.catalogo.shared.infrastructure.rest.CatedraReadExecutor;
import com.example.catalogo.sync.application.exception.CatalogSnapshotException;
import com.example.catalogo.sync.domain.model.CatalogSnapshot;
import com.example.catalogo.sync.infrastructure.rest.mapper.CatalogSnapshotMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

// Sin Spring, contra una cátedra falsa (WireMock) que responde el snapshot real que publicó la
// cátedra (resources/catedra/snapshot.json). Los casos borde se arman modificando una copia.
// El reintento y los mensajes de falla se prueban en CatedraReadExecutorTest (ADR-0063).
class CatedraSnapshotAdapterTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final GenericContainer<?> catedra = FakeCatedra.newContainer();

	private CatedraSnapshotAdapter adapter;

	@BeforeAll
	static void startCatedra() {
		catedra.start();
	}

	@AfterAll
	static void stopCatedra() {
		catedra.stop();
	}

	@BeforeEach
	void setUp() {
		FakeCatedra.reset(catedra);
		CatedraProperties properties = new CatedraProperties(FakeCatedra.baseUrl(catedra), FakeCatedra.TECHNICAL_TOKEN,
				Duration.ofSeconds(1), Duration.ofSeconds(2));
		RestClient client = new CatedraRestClientConfig().catedraRestClient(properties);
		adapter = new CatedraSnapshotAdapter(client, new CatedraReadExecutor(), new CatalogSnapshotMapper());
	}

	@Test
	void readsTheRealCatedraSnapshot() {
		serve(realSnapshot());

		CatalogSnapshot snapshot = adapter.getSnapshot();

		assertThat(snapshot.getSnapshotVersion()).isEqualTo(1L);
		assertThat(snapshot.getCategories()).hasSize(8);
		assertThat(snapshot.getProfessionals()).hasSize(24);
		assertThat(snapshot.getWeeklySchedules()).hasSize(44);
		// El snapshot trae también las entidades deshabilitadas (REF §7).
		assertThat(snapshot.getProfessionals()).filteredOn(p -> !p.isEnabled()).hasSize(3);
		assertThat(snapshot.getWeeklySchedules()).filteredOn(s -> !s.isEnabled()).hasSize(3);
		assertThat(snapshot.getWeeklySchedules()).extracting(WeeklySchedule::getSlotDurationMinutes).containsOnly(20, 30);
	}

	@Test
	void mapsEveryFieldOfEachCollection() {
		serve(realSnapshot());

		CatalogSnapshot snapshot = adapter.getSnapshot();

		Category category = snapshot.getCategories().getFirst();
		assertThat(category.getId()).isEqualTo(101L);
		assertThat(category.getName()).isEqualTo("Cardiologia");
		assertThat(category.getDescription()).isEqualTo("Atencion cardiovascular para el escenario academico");
		assertThat(category.isEnabled()).isTrue();
		assertThat(category.getCreatedAt()).isEqualTo(Instant.parse("2026-09-01T03:00:00Z"));
		assertThat(category.getUpdatedAt()).isEqualTo(Instant.parse("2026-09-01T03:00:00Z"));

		Professional professional = snapshot.getProfessionals().getFirst();
		assertThat(professional.getId()).isEqualTo(201L);
		assertThat(professional.getCategoryId()).isEqualTo(101L);
		assertThat(professional.getFirstName()).isEqualTo("Ana");
		assertThat(professional.getLastName()).isEqualTo("Torres");

		WeeklySchedule schedule = snapshot.getWeeklySchedules().getFirst();
		assertThat(schedule.getId()).isEqualTo(301L);
		assertThat(schedule.getProfessionalId()).isEqualTo(201L);
		assertThat(schedule.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
		assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(12, 0));
		assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(15, 0));
		assertThat(schedule.getSlotDurationMinutes()).isEqualTo(30);
	}

	@Test
	void acceptsHoursWithoutSeconds() {
		// La cátedra hoy manda "HH:mm:ss", pero la REF §4 admite también "HH:mm".
		ObjectNode snapshot = realSnapshot();
		ObjectNode schedule = (ObjectNode) snapshot.get("weeklySchedules").get(0);
		schedule.put("startTime", "12:00");
		schedule.put("endTime", "15:30");
		serve(snapshot);

		WeeklySchedule read = adapter.getSnapshot().getWeeklySchedules().getFirst();

		assertThat(read.getStartTime()).isEqualTo(LocalTime.of(12, 0));
		assertThat(read.getEndTime()).isEqualTo(LocalTime.of(15, 30));
	}

	@Test
	void ignoresFieldsItDoesNotKnow() {
		// La cátedra puede agregar campos sin cambiar de versión; no tienen que romper la lectura.
		ObjectNode snapshot = realSnapshot();
		snapshot.put("newRootField", "x");
		((ObjectNode) snapshot.get("professionals").get(0)).put("newField", 1);
		serve(snapshot);

		assertThat(adapter.getSnapshot().getProfessionals()).hasSize(24);
	}

	@Test
	void rejectsADayOfWeekOutsideTheContract() {
		ObjectNode snapshot = realSnapshot();
		((ObjectNode) snapshot.get("weeklySchedules").get(0)).put("dayOfWeek", "HOLIDAY");
		serve(snapshot);

		assertThatThrownBy(adapter::getSnapshot)
				.isInstanceOf(CatalogSnapshotException.class)
				.hasMessage("Unexpected response from the catedra: unreadable body");
	}

	@Test
	void rejectsASnapshotWithoutVersion() {
		ObjectNode snapshot = realSnapshot();
		snapshot.remove("snapshotVersion");
		serve(snapshot);

		assertThatThrownBy(adapter::getSnapshot)
				.isInstanceOf(CatalogSnapshotException.class)
				.hasMessage("Unexpected response from the catedra: incomplete snapshot");
	}

	@Test
	void rejectsASnapshotWithoutOneOfTheCollections() {
		ObjectNode snapshot = realSnapshot();
		snapshot.remove("weeklySchedules");
		serve(snapshot);

		assertThatThrownBy(adapter::getSnapshot)
				.isInstanceOf(CatalogSnapshotException.class)
				.hasMessage("Unexpected response from the catedra: incomplete snapshot");
	}

	@Test
	void acceptsAnEmptyCollection() {
		// Vacía no es lo mismo que ausente: un catálogo sin horarios es un catálogo válido.
		ObjectNode snapshot = realSnapshot();
		snapshot.set("weeklySchedules", JSON.createArrayNode());
		serve(snapshot);

		assertThat(adapter.getSnapshot().getWeeklySchedules()).isEmpty();
	}

	@Test
	void reportsAFailureOfTheCatedraWithTheCommonMessage() {
		FakeCatedra.stub(catedra, """
				{ "request": { "method": "GET", "url": "%s" }, "response": { "status": 503 } }
				""".formatted(CatedraSnapshotAdapter.SNAPSHOT_PATH));

		assertThatThrownBy(adapter::getSnapshot)
				.isInstanceOf(CatalogSnapshotException.class)
				.hasMessage("Catedra service unavailable (HTTP 503)");
	}

	private static ObjectNode realSnapshot() {
		return (ObjectNode) JSON.readTree(FakeCatedra.readResource(FakeCatedra.SNAPSHOT));
	}

	private static void serve(ObjectNode snapshot) {
		FakeCatedra.stub(catedra, FakeCatedra.snapshotMapping(snapshot.toString()));
	}

}
