package com.example.catalogo.professional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.example.catalogo.CatalogTables;
import com.example.catalogo.TestCatedraConfiguration;
import com.example.catalogo.TestClockConfiguration;
import com.example.catalogo.TestJwtKeysConfiguration;
import com.example.catalogo.TestcontainersConfiguration;
import com.example.catalogo.sync.application.service.SyncService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Los cuatro filtros de la búsqueda (HU-05), cada uno solo y combinado con los demás, de punta a punta
 * y con los datos reales de la cátedra (docs/arq/pruebas.md).
 *
 * <p>Antes de cada caso se recarga el snapshot real por el flujo de sincronización de verdad y se le
 * agregan los casos borde que no trae (RNF-09):
 * <ul>
 * <li>la categoría 108 (Salud mental) deshabilitada: Leiva (220) y Reyes (221) quedan deshabilitados
 * aunque ellos estén habilitados (ADR-0014);</li>
 * <li>un apellido con tilde: Diego Álvarez (207);</li>
 * <li>un segundo horario el mismo día: Torres (201) atiende el lunes de 12 a 15 y de 17 a 19, con
 * turnos de 45 minutos;</li>
 * <li>sábados: Navarro (216) atiende de 9 a 13, con turnos de 15 minutos, y Quiroga (209) tiene un
 * horario deshabilitado.</li>
 * </ul>
 * Del snapshot, ya deshabilitados: Molina (206), Luna (214) y Mansilla (222). "Hoy" es el martes
 * 2026-10-20 ({@link TestClockConfiguration}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, TestJwtKeysConfiguration.class, TestCatedraConfiguration.class,
		TestClockConfiguration.class })
class ProfessionalSearchFiltersTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private KeyPair testJwtKeyPair;

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private SyncService syncService;

	@BeforeEach
	void loadTheRealSnapshotWithEdgeCases() {
		CatalogTables.clear(jdbcClient);
		jdbcClient.sql("UPDATE sync_state SET local_version = NULL").update();
		syncService.synchronizeCatalog();

		jdbcClient.sql("UPDATE professional_category SET enabled = false WHERE id = 108").update();
		jdbcClient.sql("UPDATE professional SET last_name = 'Álvarez' WHERE id = 207").update();
		jdbcClient.sql("""
				INSERT INTO weekly_schedule (id, professional_id, day_of_week, start_time, end_time,
					slot_duration_minutes, enabled, created_at, updated_at)
				VALUES (345, 201, 'MONDAY', '17:00', '19:00', 45, true, now(), now()),
					(346, 216, 'SATURDAY', '09:00', '13:00', 15, true, now(), now()),
					(347, 209, 'SATURDAY', '10:00', '12:00', 30, false, now(), now())
				""").update();
	}

	// Columnas: categoryId | name | enabled | date | timeFrom | timeTo | ids esperados. Vacío = sin el
	// filtro. Fechas: lunes 2026-10-26, miércoles 21, jueves 22, viernes 23, sábado 24.
	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', nullValues = "", value = {
			// --- Cada filtro solo ---
			"Sin filtros: solo los habilitados          |     |        |       |            |       |       | 201 202 203 204 205 207 208 209 210 211 212 213 215 216 217 218 219 223 224",
			"Categoría                                  | 103 |        |       |            |       |       | 207 208 209 223",
			"Nombre sin la tilde guardada               |     | alvarez|       |            |       |       | 207",
			"Nombre con una tilde que el dato no tiene  |     | MARTÍN |       |            |       |       | 202",
			"Deshabilitados, propios o por categoría    |     |        | false |            |       |       | 206 214 220 221 222",
			"Fecha: el día de la semana                 |     |        |       | 2026-10-26 |       |       | 201 204 207 209 213 215 217 224",
			"Fecha en sábado, sin el horario apagado    |     |        |       | 2026-10-24 |       |       | 216",
			"Franja: el segundo horario del día cuenta  |     |        |       | 2026-10-26 | 17:30 | 18:00 | 201 213",
			// --- De a dos ---
			"Categoría y nombre                         | 103 | ez     |       |            |       |       | 207 223",
			"Categoría deshabilitada, por defecto       | 108 |        |       |            |       |       | ",
			"Categoría deshabilitada, pidiendo false    | 108 |        | false |            |       |       | 220 221 222",
			"Nombre, habilitados                        |     | lu     |       |            |       |       | 211",
			"Nombre, deshabilitados                     |     | lu     | false |            |       |       | 214",
			"Categoría y fecha                          | 105 |        |       | 2026-10-26 |       |       | 213 224",
			"Categoría y fecha, sin resultados          | 101 |        |       | 2026-10-24 |       |       | ",
			"Deshabilitado por categoría, con agenda    |     |        | false | 2026-10-26 |       |       | 220",
			"Nombre y franja                            |     | a      |       | 2026-10-21 | 11:00 | 12:00 | 213",
			// --- Los cuatro juntos ---
			"Cuatro: franja que empieza con el horario  | 103 | ez     | true  | 2026-10-26 | 18:00 | 19:00 | 207",
			"Cuatro: deshabilitado con agenda el jueves | 108 | e      | false | 2026-10-22 |       |       | 220",
			"Cuatro: franja al final del horario        | 102 | a      | true  | 2026-10-23 | 15:00 | 16:00 | 204",
	})
	void eachFilterAloneAndCombinedReturnsExactlyTheExpectedProfessionals(String scenario, String categoryId,
			String name, String enabled, String date, String timeFrom, String timeTo, String expectedIds) throws Exception {
		var request = get("/api/professionals").param("size", "100")
				.header(HttpHeaders.AUTHORIZATION, bearer());
		param(request, "categoryId", categoryId);
		param(request, "name", name);
		param(request, "enabled", enabled);
		param(request, "date", date);
		param(request, "timeFrom", timeFrom);
		param(request, "timeTo", timeTo);

		String body = mockMvc.perform(request)
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		List<Integer> ids = JsonPath.read(body, "$[*].id");
		assertThat(ids).as(scenario).containsExactlyInAnyOrderElementsOf(expected(expectedIds));
	}

	private void param(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
			String name, String value) {
		if (value != null) {
			request.param(name, value.strip());
		}
	}

	private List<Integer> expected(String ids) {
		if (ids == null || ids.isBlank()) {
			return List.of();
		}
		return Arrays.stream(ids.strip().split("\\s+")).map(Integer::valueOf).toList();
	}

	private String bearer() {
		Instant now = Instant.now();
		return "Bearer " + TestJwtKeysConfiguration.signToken(
				testJwtKeyPair, "juan", now, now.plus(Duration.ofHours(1)), "ROLE_USER");
	}

}
