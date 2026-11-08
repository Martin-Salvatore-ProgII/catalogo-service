package com.example.catalogo.sync.infrastructure.redis.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import com.example.catalogo.FakeRedis;
import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.shared.infrastructure.config.CatedraIntegration;
import com.example.catalogo.shared.infrastructure.config.CatedraProperties;
import com.example.catalogo.shared.infrastructure.config.RedisConfig;
import com.example.catalogo.sync.application.exception.CatalogChangesException;
import com.example.catalogo.sync.domain.model.CatalogVersions;
import com.example.catalogo.sync.domain.model.VersionChanges;
import com.example.catalogo.sync.infrastructure.catedra.mapper.CatedraEntityMapper;
import com.example.catalogo.sync.infrastructure.redis.mapper.VersionChangesMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import tools.jackson.databind.json.JsonMapper;

// Sin Spring, contra un Redis real cargado con el formato de la REF §14 y los datos del snapshot
// real de la cátedra (ADR-0044, ADR-0045). Los casos borde se arman sobre lo cargado.
class RedisCatalogAdapterTest {

	private static final GenericContainer<?> redis = FakeRedis.newContainer();

	private static StringRedisTemplate template;
	private static RedisCatalogAdapter adapter;

	@BeforeAll
	static void startRedis() {
		redis.start();
		template = FakeRedis.templateFor(redis);
		adapter = newAdapter(template);
	}

	@AfterAll
	static void stopRedis() {
		redis.stop();
	}

	@BeforeEach
	void loadCatalog() {
		FakeRedis.clear(template);
		FakeRedis.publishSnapshotAsVersion(template, 7);
	}

	private static RedisCatalogAdapter newAdapter(StringRedisTemplate redisTemplate) {
		return new RedisCatalogAdapter(redisTemplate, JsonMapper.builder().build(), new VersionChangesMapper(),
				new CatedraEntityMapper());
	}

	@Test
	void readsCurrentAndOldestVersion() {
		FakeRedis.publishVersions(template, 9, 4);

		CatalogVersions versions = adapter.getVersions();

		assertThat(versions.getCurrent()).isEqualTo(9);
		assertThat(versions.getOldestAvailable()).isEqualTo(4);
	}

	@Test
	void failsWhenAVersionIsMissingBecauseThereIsNothingToCompareWith() {
		template.delete("catedra:sync:oldest-available-version");

		assertThatThrownBy(() -> adapter.getVersions())
				.isInstanceOf(CatalogChangesException.class)
				.hasMessage("Unexpected response from the catedra Redis: missing catedra:sync:oldest-available-version");
	}

	@Test
	void failsWhenAVersionIsNotANumber() {
		template.opsForValue().set("catedra:sync:current-version", "siete");

		assertThatThrownBy(() -> adapter.getVersions())
				.isInstanceOf(CatalogChangesException.class)
				.hasMessage("Unexpected response from the catedra Redis: unreadable catedra:sync:current-version");
	}

	@Test
	void readsTheIdsAffectedByAVersion() {
		FakeRedis.putChanges(template, 8, "[]", "[201]", "[301,302]");

		VersionChanges changes = adapter.findChanges(8).orElseThrow();

		assertThat(changes.getVersion()).isEqualTo(8);
		assertThat(changes.getCategoryIds()).isEmpty();
		assertThat(changes.getProfessionalIds()).containsExactly(201L);
		assertThat(changes.getWeeklyScheduleIds()).containsExactly(301L, 302L);
	}

	@Test
	void readsTheRealChangesOfTheSnapshotVersion() {
		VersionChanges changes = adapter.findChanges(7).orElseThrow();

		assertThat(changes.getCategoryIds()).hasSize(8);
		assertThat(changes.getProfessionalIds()).hasSize(24);
		assertThat(changes.getWeeklyScheduleIds()).hasSize(44);
	}

	@Test
	void ignoresUnknownFieldsInTheChanges() {
		template.opsForValue().set("catedra:sync:changes:8",
				"{\"version\":8,\"extra\":true,\"changes\":{\"professionalCategories\":[],\"professionals\":[201],\"weeklySchedules\":[],\"other\":[1]}}");

		assertThat(adapter.findChanges(8)).isPresent();
	}

	@Test
	void answersEmptyWhenTheVersionHasNoChanges() {
		assertThat(adapter.findChanges(99)).isEmpty();
	}

	@Test
	void answersEmptyWhenTheChangesAreUnreadable() {
		template.opsForValue().set("catedra:sync:changes:8", "esto no es JSON");

		assertThat(adapter.findChanges(8)).isEmpty();
	}

	@Test
	void answersEmptyWhenTheChangesAreForAnotherVersion() {
		template.opsForValue().set("catedra:sync:changes:8",
				"{\"version\":9,\"changes\":{\"professionalCategories\":[],\"professionals\":[],\"weeklySchedules\":[]}}");

		assertThat(adapter.findChanges(8)).isEmpty();
	}

	@Test
	void answersEmptyWhenAListOfChangesIsMissing() {
		template.opsForValue().set("catedra:sync:changes:8",
				"{\"version\":8,\"changes\":{\"professionalCategories\":[],\"professionals\":[]}}");

		assertThat(adapter.findChanges(8)).isEmpty();
	}

	@Test
	void readsCategoriesWithTheirCurrentState() {
		List<Category> categories = adapter.findCategories(List.of(101L, 102L));

		assertThat(categories).extracting(Category::getId).containsExactlyInAnyOrder(101L, 102L);
		assertThat(categories).allSatisfy(category -> {
			assertThat(category.getName()).isNotBlank();
			assertThat(category.getCreatedAt()).isNotNull();
		});
	}

	@Test
	void readsProfessionals() {
		List<Professional> professionals = adapter.findProfessionals(List.of(201L));

		assertThat(professionals).hasSize(1);
		assertThat(professionals.get(0).getCategoryId()).isNotNull();
		assertThat(professionals.get(0).getLastName()).isNotBlank();
	}

	@Test
	void readsWeeklySchedulesWithTheirHours() {
		List<WeeklySchedule> schedules = adapter.findWeeklySchedules(List.of(301L));

		assertThat(schedules).hasSize(1);
		assertThat(schedules.get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
		assertThat(schedules.get(0).getStartTime()).isEqualTo(LocalTime.of(12, 0));
		assertThat(schedules.get(0).getSlotDurationMinutes()).isEqualTo(30);
	}

	@Test
	void readsDisabledEntitiesAsTheyAreBecauseDeletionsAreLogical() {
		template.opsForHash().put("catedra:sync:professionals", "201",
				"{\"id\":201,\"categoryId\":101,\"firstName\":\"Ana\",\"lastName\":\"Gomez\",\"enabled\":false,"
						+ "\"createdAt\":\"2026-07-01T12:00:00Z\",\"updatedAt\":\"2026-09-10T13:40:00Z\"}");

		Professional professional = adapter.findProfessionals(List.of(201L)).get(0);

		assertThat(professional.isEnabled()).isFalse();
	}

	@Test
	void acceptsHoursWithAndWithoutSeconds() {
		template.opsForHash().put("catedra:sync:weekly-schedules", "301",
				"{\"id\":301,\"professionalId\":201,\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"12:00:00\","
						+ "\"slotDurationMinutes\":20,\"enabled\":true,\"createdAt\":\"2026-07-01T12:00:00Z\",\"updatedAt\":\"2026-09-10T13:40:00Z\"}");

		WeeklySchedule schedule = adapter.findWeeklySchedules(List.of(301L)).get(0);

		assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(9, 0));
		assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(12, 0));
	}

	@Test
	void leavesOutTheIdsThatAreNotInTheHash() {
		List<Category> categories = adapter.findCategories(List.of(101L, 999L));

		assertThat(categories).extracting(Category::getId).containsExactly(101L);
	}

	@Test
	void leavesOutTheEntitiesThatCannotBeRead() {
		template.opsForHash().put("catedra:sync:professional-categories", "102", "{no es json");

		List<Category> categories = adapter.findCategories(List.of(101L, 102L));

		assertThat(categories).extracting(Category::getId).containsExactly(101L);
	}

	@Test
	void leavesOutAnEntityWhoseIdIsNotItsField() {
		template.opsForHash().put("catedra:sync:professional-categories", "102",
				"{\"id\":103,\"name\":\"Otra\",\"enabled\":true,\"createdAt\":\"2026-07-01T12:00:00Z\",\"updatedAt\":\"2026-07-01T12:00:00Z\"}");

		List<Category> categories = adapter.findCategories(List.of(102L));

		assertThat(categories).isEmpty();
	}

	@Test
	void answersEmptyWithoutAskingRedisWhenThereAreNoIds() {
		assertThat(adapter.findProfessionals(List.of())).isEmpty();
	}

	@Test
	void readsWithoutWritingAnythingToRedis() {
		long keysBefore = template.getConnectionFactory().getConnection().serverCommands().dbSize();

		adapter.getVersions();
		adapter.findChanges(7);
		adapter.findCategories(List.of(101L));

		assertThat(template.getConnectionFactory().getConnection().serverCommands().dbSize()).isEqualTo(keysBefore);
	}

	@Test
	void reportsAFailureWithoutRevealingTheAddressWhenRedisDoesNotAnswer() {
		// Puerto cerrado: se prueba también la fábrica real del servicio (RedisConfig).
		CatedraIntegration integration = new CatedraIntegration("grupo", "127.0.0.1", 1, "usuario", "clave-secreta",
				"kafka:9092", "grupo", "topic", "PROVISIONED");
		CatedraProperties properties = new CatedraProperties("http://api", "jwt", Duration.ofMillis(500), Duration.ofMillis(500));
		LettuceConnectionFactory factory =
				(LettuceConnectionFactory) new RedisConfig().catedraRedisConnectionFactory(integration, properties);
		factory.afterPropertiesSet();
		RedisCatalogAdapter broken = newAdapter(new StringRedisTemplate(factory));

		assertThatThrownBy(broken::getVersions)
				.isInstanceOf(CatalogChangesException.class)
				.hasMessageStartingWith("Catedra Redis unavailable (")
				.message().doesNotContain("127.0.0.1").doesNotContain("clave-secreta");
	}

	// La conexión real del servicio (RedisConfig), con el usuario y la ACL de la cátedra: lee
	// catedra:sync:* y no puede escribir ahí (REF §14.1).
	@Test
	void readsWithTheCatedraAclUserWhichCannotWriteTheSyncKeys() {
		CatedraIntegration integration = new CatedraIntegration("proyecto-de-prueba", redis.getHost(), redis.getMappedPort(6379),
				"grp_proyecto-de-prueba", "clave-redis-de-prueba", "kafka:9092", "grupo", "topic", "PROVISIONED");
		CatedraProperties properties = new CatedraProperties("http://api", "jwt", Duration.ofSeconds(2), Duration.ofSeconds(5));
		LettuceConnectionFactory factory =
				(LettuceConnectionFactory) new RedisConfig().catedraRedisConnectionFactory(integration, properties);
		factory.afterPropertiesSet();
		StringRedisTemplate aclTemplate = new StringRedisTemplate(factory);
		aclTemplate.afterPropertiesSet();

		assertThat(newAdapter(aclTemplate).getVersions().getCurrent()).isEqualTo(7);
		assertThatThrownBy(() -> aclTemplate.opsForValue().set("catedra:sync:current-version", "99"))
				.hasRootCauseMessage("NOPERM No permissions to access a key");
	}

}
