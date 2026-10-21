package com.example.catalogo.professional;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.time.Duration;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.example.catalogo.CatalogTables;
import com.example.catalogo.TestCatedraConfiguration;
import com.example.catalogo.TestJwtKeysConfiguration;
import com.example.catalogo.TestcontainersConfiguration;
import com.example.catalogo.professional.domain.model.AgendaZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La búsqueda de profesionales con la aplicación completa: seguridad, migraciones (incluida la
 * extensión unaccent), transacción, PostgreSQL y la respuesta JSON reales. Los datos se guardan con
 * SQL, sin depender de las clases de otra feature (ADR-0062).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, TestJwtKeysConfiguration.class, TestCatedraConfiguration.class })
class ProfessionalSearchAccessTest {

	private static final String PROFESSIONALS = "/api/professionals";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private KeyPair testJwtKeyPair;

	@Autowired
	private JdbcClient jdbcClient;

	// Al arrancar, el servicio cargó el snapshot de la cátedra falsa: se reemplaza por datos propios.
	@BeforeEach
	void storeProfessionals() {
		CatalogTables.clear(jdbcClient);
		jdbcClient.sql("""
				INSERT INTO professional_category (id, name, description, enabled, created_at, updated_at)
				VALUES (101, 'Cardiologia', 'Corazon', true, now(), now()), (102, 'Pediatria', 'Niños', true, now(), now()),
					(103, 'Dermatologia', 'Piel', false, now(), now())
				""").update();
		jdbcClient.sql("""
				INSERT INTO professional (id, category_id, first_name, last_name, enabled, created_at, updated_at)
				VALUES (201, 101, 'Ana', 'Gómez', true, now(), now()),
					(202, 101, 'Martin', 'Rivas', true, now(), now()),
					(203, 102, 'Lucía', 'Paz', true, now(), now()),
					(204, 102, 'Bruno', 'Torres', false, now(), now()),
					(205, 103, 'Nicolas', 'Vega', true, now(), now())
				""").update();
	}

	// Estas pruebas guardan datos de verdad, sin transacción que los deshaga: se limpian al terminar.
	@AfterEach
	void removeProfessionals() {
		CatalogTables.clear(jdbcClient);
		// El contexto lo comparten otras pruebas, que esperan la versión del arranque.
		jdbcClient.sql("UPDATE sync_state SET local_version = 1").update();
	}

	@Test
	void withoutTokenIsRejected() throws Exception {
		mockMvc.perform(get(PROFESSIONALS))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(""));
	}

	@Test
	void findsByPartialNameWithoutCaseOrAccents() throws Exception {
		mockMvc.perform(get(PROFESSIONALS).param("name", "ANA GOM").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Total-Count", "1"))
				.andExpect(jsonPath("$[0].id").value(201))
				.andExpect(jsonPath("$[0].lastName").value("Gómez"))
				.andExpect(jsonPath("$[0].category.name").value("Cardiologia"));
	}

	@Test
	void combinesCategoryAndNameOrderedByLastNameByDefault() throws Exception {
		mockMvc.perform(get(PROFESSIONALS).param("categoryId", "101").param("name", "a")
				.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Total-Count", "2"))
				.andExpect(jsonPath("$[0].lastName").value("Gómez"))
				.andExpect(jsonPath("$[1].lastName").value("Rivas"));
	}

	@Test
	void paginatesWithTheTotalOfAllPages() throws Exception {
		mockMvc.perform(get(PROFESSIONALS).param("size", "2").param("page", "1").param("sort", "firstName,desc")
				.header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Total-Count", "3"))
				// Por nombre descendente: Martin, Lucía | Ana.
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].firstName").value("Ana"));
	}

	@Test
	void byDefaultHidesTheDisabledOnesAndThoseOfADisabledCategory() throws Exception {
		mockMvc.perform(get(PROFESSIONALS).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Total-Count", "3"))
				.andExpect(jsonPath("$[*].enabled", everyItem(is(true))));
	}

	@Test
	void showsTheDisabledOnesMarkedAsNotAvailableWhenAsked() throws Exception {
		mockMvc.perform(get(PROFESSIONALS).param("enabled", "false").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Total-Count", "2"))
				.andExpect(jsonPath("$[0].lastName").value("Torres"))
				.andExpect(jsonPath("$[0].enabled").value(false))
				// Vega está habilitado, pero su categoría no (ADR-0014).
				.andExpect(jsonPath("$[1].lastName").value("Vega"))
				.andExpect(jsonPath("$[1].enabled").value(false));
	}

	@Test
	void filtersByAvailabilityOfTheAgenda() throws Exception {
		jdbcClient.sql("""
				INSERT INTO weekly_schedule (id, professional_id, day_of_week, start_time, end_time,
					slot_duration_minutes, enabled, created_at, updated_at)
				VALUES (301, 201, 'MONDAY', '09:00', '12:00', 30, true, now(), now()),
					(302, 202, 'MONDAY', '14:00', '18:00', 30, true, now(), now())
				""").update();
		// El próximo lunes, siempre dentro del horizonte.
		LocalDate monday = LocalDate.now(AgendaZone.ZONE).with(TemporalAdjusters.next(DayOfWeek.MONDAY));

		mockMvc.perform(get(PROFESSIONALS).param("date", monday.toString()).param("timeFrom", "10:00")
				.param("timeTo", "11:00").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Total-Count", "1"))
				.andExpect(jsonPath("$[0].lastName").value("Gómez"));
	}

	@Test
	void rejectsAPastDate() throws Exception {
		LocalDate yesterday = LocalDate.now(AgendaZone.ZONE).minusDays(1);

		mockMvc.perform(get(PROFESSIONALS).param("date", yesterday.toString()).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("DATE_OUT_OF_RANGE"));
	}

	// --- Agenda vigente (ADR-0017) ---

	@Test
	void theAgendaRequiresAToken() throws Exception {
		mockMvc.perform(get(PROFESSIONALS + "/201/agenda"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(""));
	}

	@Test
	void theAgendaReturnsTheEnabledSchedulesInOrderAndTheLocalVersion() throws Exception {
		jdbcClient.sql("""
				INSERT INTO weekly_schedule (id, professional_id, day_of_week, start_time, end_time,
					slot_duration_minutes, enabled, created_at, updated_at)
				VALUES (303, 201, 'FRIDAY', '09:00', '12:00', 30, true, now(), now()),
					(302, 201, 'MONDAY', '15:00', '18:00', 20, true, now(), now()),
					(301, 201, 'MONDAY', '09:00', '12:00', 30, true, now(), now()),
					(304, 201, 'TUESDAY', '09:00', '12:00', 30, false, now(), now())
				""").update();
		jdbcClient.sql("UPDATE sync_state SET local_version = 7").update();

		mockMvc.perform(get(PROFESSIONALS + "/201/agenda").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lastName").value("Gómez"))
				.andExpect(jsonPath("$.category.name").value("Cardiologia"))
				.andExpect(jsonPath("$.enabled").value(true))
				.andExpect(jsonPath("$.weeklySchedules[*].id", contains(301, 302, 303)))
				.andExpect(jsonPath("$.weeklySchedules[0].startTime").value("09:00:00"))
				.andExpect(jsonPath("$.catalogVersion").value(7));
	}

	@Test
	void theAgendaOfAProfessionalInADisabledCategoryIsReturnedAsNotEnabled() throws Exception {
		mockMvc.perform(get(PROFESSIONALS + "/205/agenda").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(false));
	}

	@Test
	void theAgendaOfAnUnknownProfessionalIsNotFound() throws Exception {
		mockMvc.perform(get(PROFESSIONALS + "/999/agenda").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PROFESSIONAL_NOT_FOUND"));
	}

	@Test
	void rejectsAnInvalidFilter() throws Exception {
		mockMvc.perform(get(PROFESSIONALS).param("size", "500").header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
	}

	private String bearer() {
		Instant now = Instant.now();
		return "Bearer " + TestJwtKeysConfiguration.signToken(
				testJwtKeyPair, "juan", now, now.plus(Duration.ofHours(1)), "ROLE_USER");
	}

}
