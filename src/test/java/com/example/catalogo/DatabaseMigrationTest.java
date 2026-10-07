package com.example.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica que las migraciones de Flyway dejan el esquema esperado en PostgreSQL. Los datos de
 * ejemplo tienen la forma de los que publica la cátedra. Cada prueba se deshace al terminar.
 */
@Import({ TestcontainersConfiguration.class, TestJwtKeysConfiguration.class })
@SpringBootTest
@Transactional
class DatabaseMigrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Test
	void createsTheCatalogTables() {
		List<String> tables = jdbcClient
				.sql("SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")
				.query(String.class)
				.list();

		assertThat(tables).contains("professional_category", "professional", "weekly_schedule");
	}

	@Test
	void storesACategoryWithItsProfessionalAndSchedule() {
		insertCategory(101, "Cardiologia", "Atencion cardiovascular para el escenario academico");
		insertProfessional(201, 101);
		insertSchedule(301, 201, "MONDAY");

		Integer slotDuration = jdbcClient.sql("""
				SELECT s.slot_duration_minutes
				FROM weekly_schedule s
				JOIN professional p ON p.id = s.professional_id
				JOIN professional_category c ON c.id = p.category_id
				WHERE c.id = 101
				""").query(Integer.class).single();

		assertThat(slotDuration).isEqualTo(30);
	}

	@Test
	void descriptionIsOptional() {
		insertCategory(101, "Cardiologia", null);

		assertThat(jdbcClient.sql("SELECT count(*) FROM professional_category").query(Integer.class).single()).isEqualTo(1);
	}

	@Test
	void acceptsTextsOfAnyLength() {
		insertCategory(101, "x".repeat(500), "y".repeat(5000));

		assertThat(jdbcClient.sql("SELECT length(description) FROM professional_category WHERE id = 101")
				.query(Integer.class).single()).isEqualTo(5000);
	}

	@Test
	void doesNotGenerateIds() {
		assertThatThrownBy(() -> jdbcClient.sql("""
				INSERT INTO professional_category (name, enabled, created_at, updated_at)
				VALUES ('Cardiologia', true, now(), now())
				""").update()).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsAProfessionalOfAnUnknownCategory() {
		assertThatThrownBy(() -> insertProfessional(201, 999)).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsAScheduleOfAnUnknownProfessional() {
		assertThatThrownBy(() -> insertSchedule(301, 999, "MONDAY")).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void rejectsADayOfWeekOutsideTheContract() {
		insertCategory(101, "Cardiologia", null);
		insertProfessional(201, 101);

		assertThatThrownBy(() -> insertSchedule(301, 201, "LUNES"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_weekly_schedule_day_of_week");
	}

	private void insertCategory(long id, String name, String description) {
		jdbcClient.sql("""
				INSERT INTO professional_category (id, name, description, enabled, created_at, updated_at)
				VALUES (:id, :name, :description, true, '2026-09-01T03:00:00Z', '2026-09-01T03:00:00Z')
				""").param("id", id).param("name", name).param("description", description).update();
	}

	private void insertProfessional(long id, long categoryId) {
		jdbcClient.sql("""
				INSERT INTO professional (id, category_id, first_name, last_name, enabled, created_at, updated_at)
				VALUES (:id, :categoryId, 'Ana', 'Torres', true, '2026-09-01T03:00:00Z', '2026-09-01T03:00:00Z')
				""").param("id", id).param("categoryId", categoryId).update();
	}

	private void insertSchedule(long id, long professionalId, String dayOfWeek) {
		jdbcClient.sql("""
				INSERT INTO weekly_schedule (id, professional_id, day_of_week, start_time, end_time,
						slot_duration_minutes, enabled, created_at, updated_at)
				VALUES (:id, :professionalId, :dayOfWeek, '12:00:00', '15:00:00', 30, true,
						'2026-09-01T03:00:00Z', '2026-09-01T03:00:00Z')
				""").param("id", id).param("professionalId", professionalId).param("dayOfWeek", dayOfWeek).update();
	}

}
