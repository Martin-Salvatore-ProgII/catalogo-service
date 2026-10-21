package com.example.catalogo.professional.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import com.example.catalogo.TestcontainersConfiguration;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.professional.infrastructure.persistence.entity.WeeklyScheduleEntity;
import com.example.catalogo.professional.infrastructure.persistence.mapper.WeeklyScheduleMapper;
import com.example.catalogo.professional.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

// Prueba el adaptador contra PostgreSQL real, con las migraciones de Flyway (ADR-0045).
// Cada test corre en una transacción que se deshace al terminar.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, JpaWeeklyScheduleRepositoryAdapter.class, WeeklyScheduleMapper.class })
class JpaWeeklyScheduleRepositoryAdapterTest {

	private static final Instant CREATED_AT = Instant.parse("2026-09-01T03:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-09-10T13:40:00Z");

	@Autowired
	private JpaWeeklyScheduleRepositoryAdapter adapter;

	@Autowired
	private JpaWeeklyScheduleRepository jpaWeeklyScheduleRepository;

	@Autowired
	private TestEntityManager entityManager;

	// El horario necesita su profesional, y este su categoría (otra feature): se crean con SQL.
	@BeforeEach
	void storeProfessional() {
		entityManager.getEntityManager().createNativeQuery("""
				INSERT INTO professional_category (id, name, enabled, created_at, updated_at)
				VALUES (101, 'Cardiologia', true, now(), now())
				""").executeUpdate();
		entityManager.getEntityManager().createNativeQuery("""
				INSERT INTO professional (id, category_id, first_name, last_name, enabled, created_at, updated_at)
				VALUES (201, 101, 'Ana', 'Torres', true, now(), now())
				""").executeUpdate();
	}

	@Test
	void savesSchedulesWithEveryField() {
		// Slots de 20 y de 30 minutos, como en el catálogo real de la cátedra (RNF-09).
		adapter.saveAll(List.of(
				schedule(301L, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(15, 0), 30),
				schedule(302L, DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(11, 20), 20)));
		readFromDatabase();

		WeeklyScheduleEntity saved = jpaWeeklyScheduleRepository.findById(302L).orElseThrow();
		assertThat(jpaWeeklyScheduleRepository.count()).isEqualTo(2);
		assertThat(saved.getProfessionalId()).isEqualTo(201L);
		assertThat(saved.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
		assertThat(saved.getStartTime()).isEqualTo(LocalTime.of(8, 0));
		assertThat(saved.getEndTime()).isEqualTo(LocalTime.of(11, 20));
		assertThat(saved.getSlotDurationMinutes()).isEqualTo(20);
		assertThat(saved.isEnabled()).isTrue();
		assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);
		assertThat(saved.getUpdatedAt()).isEqualTo(UPDATED_AT);
	}

	@Test
	void storesTheDayOfWeekAsTheNameTheCatedraUses() {
		adapter.saveAll(List.of(schedule(301L, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30)));
		readFromDatabase();

		Object stored = entityManager.getEntityManager()
				.createNativeQuery("SELECT day_of_week FROM weekly_schedule WHERE id = 301")
				.getSingleResult();
		assertThat(stored).isEqualTo("WEDNESDAY");
	}

	@Test
	void replacesAScheduleThatIsAlreadyStored() {
		adapter.saveAll(List.of(schedule(301L, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(15, 0), 30)));
		readFromDatabase();

		// La cátedra le cambió el horario y la duración del slot.
		adapter.saveAll(List.of(schedule(301L, DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(18, 0), 20)));
		readFromDatabase();

		WeeklyScheduleEntity saved = jpaWeeklyScheduleRepository.findById(301L).orElseThrow();
		assertThat(jpaWeeklyScheduleRepository.count()).isEqualTo(1);
		assertThat(saved.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
		assertThat(saved.getStartTime()).isEqualTo(LocalTime.of(14, 0));
		assertThat(saved.getSlotDurationMinutes()).isEqualTo(20);
	}

	@Test
	void deletesAllSchedules() {
		adapter.saveAll(List.of(
				schedule(301L, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(15, 0), 30),
				schedule(302L, DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(11, 20), 20)));
		readFromDatabase();

		adapter.deleteAll();

		assertThat(jpaWeeklyScheduleRepository.count()).isZero();
	}

	@Test
	void savesTheSameIdsAgainAfterDeletingAllInTheSameTransaction() {
		adapter.saveAll(List.of(schedule(301L, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(15, 0), 30)));
		readFromDatabase();

		adapter.deleteAll();
		adapter.saveAll(List.of(schedule(301L, DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(15, 0), 30)));
		readFromDatabase();

		assertThat(jpaWeeklyScheduleRepository.findById(301L).orElseThrow().getStartTime()).isEqualTo(LocalTime.of(13, 0));
	}

	// --- Horarios de la agenda ---

	@Test
	void findsTheEnabledSchedulesOrderedByDayOfTheWeekAndStartTime() {
		// Guardados desordenados: alfabéticamente, FRIDAY quedaría antes que MONDAY.
		adapter.saveAll(List.of(
				schedule(303L, DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30),
				schedule(302L, DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(18, 0), 30),
				schedule(301L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30),
				schedule(304L, DayOfWeek.SUNDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), 15)));
		readFromDatabase();

		assertThat(adapter.findEnabledByProfessionalOrderedByDayAndTime(201L))
				.extracting(WeeklySchedule::getId).containsExactly(301L, 302L, 303L, 304L);
	}

	@Test
	void leavesOutDisabledSchedulesAndThoseOfOtherProfessionals() {
		entityManager.getEntityManager().createNativeQuery("""
				INSERT INTO professional (id, category_id, first_name, last_name, enabled, created_at, updated_at)
				VALUES (202, 101, 'Bruno', 'Paz', true, now(), now())
				""").executeUpdate();
		WeeklySchedule disabled = schedule(302L, DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(18, 0), 30);
		disabled.setEnabled(false);
		WeeklySchedule another = schedule(303L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30);
		another.setProfessionalId(202L);
		adapter.saveAll(List.of(schedule(301L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30),
				disabled, another));
		readFromDatabase();

		assertThat(adapter.findEnabledByProfessionalOrderedByDayAndTime(201L))
				.extracting(WeeklySchedule::getId).containsExactly(301L);
	}

	@Test
	void returnsAnEmptyListForAProfessionalWithoutSchedules() {
		assertThat(adapter.findEnabledByProfessionalOrderedByDayAndTime(201L)).isEmpty();
	}

	private WeeklySchedule schedule(Long id, DayOfWeek day, LocalTime start, LocalTime end, int slotMinutes) {
		return WeeklySchedule.builder()
				.id(id)
				.professionalId(201L)
				.dayOfWeek(day)
				.startTime(start)
				.endTime(end)
				.slotDurationMinutes(slotMinutes)
				.enabled(true)
				.createdAt(CREATED_AT)
				.updatedAt(UPDATED_AT)
				.build();
	}

	// Sin esto JPA respondería desde memoria y no se vería lo que quedó en PostgreSQL.
	private void readFromDatabase() {
		entityManager.flush();
		entityManager.clear();
	}

}
