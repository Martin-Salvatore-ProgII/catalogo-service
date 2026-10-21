package com.example.catalogo.professional.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import com.example.catalogo.TestcontainersConfiguration;
import com.example.catalogo.professional.domain.model.PageQuery;
import com.example.catalogo.professional.domain.model.PageResult;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.ProfessionalSearchCriteria;
import com.example.catalogo.professional.domain.model.ProfessionalSortField;
import com.example.catalogo.professional.domain.model.ProfessionalSortOrder;
import com.example.catalogo.professional.infrastructure.persistence.entity.ProfessionalEntity;
import com.example.catalogo.professional.infrastructure.persistence.mapper.ProfessionalMapper;
import com.example.catalogo.professional.infrastructure.persistence.repository.JpaProfessionalRepository;
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
@Import({ TestcontainersConfiguration.class, JpaProfessionalRepositoryAdapter.class, ProfessionalMapper.class })
class JpaProfessionalRepositoryAdapterTest {

	private static final Instant CREATED_AT = Instant.parse("2026-09-01T03:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-09-10T13:40:00Z");
	private static final PageQuery FIRST_PAGE = PageQuery.builder().page(0).size(20).build();
	private static final List<ProfessionalSortOrder> BY_LAST_NAME = List.of(sortBy(ProfessionalSortField.LAST_NAME, false),
			sortBy(ProfessionalSortField.FIRST_NAME, false));

	@Autowired
	private JpaProfessionalRepositoryAdapter adapter;

	@Autowired
	private JpaProfessionalRepository jpaProfessionalRepository;

	@Autowired
	private TestEntityManager entityManager;

	// La categoría es de otra feature: se crea con SQL para no depender de sus clases (ADR-0062).
	@BeforeEach
	void storeCategories() {
		entityManager.getEntityManager().createNativeQuery("""
				INSERT INTO professional_category (id, name, enabled, created_at, updated_at)
				VALUES (101, 'Cardiologia', true, now(), now()), (102, 'Pediatria', true, now(), now()),
					(103, 'Dermatologia', false, now(), now())
				""").executeUpdate();
	}

	@Test
	void savesProfessionalsWithEveryField() {
		adapter.saveAll(List.of(professional(201L, 101L, "Ana", true), professional(202L, 102L, "Bruno", false)));
		readFromDatabase();

		ProfessionalEntity saved = jpaProfessionalRepository.findById(202L).orElseThrow();
		assertThat(jpaProfessionalRepository.count()).isEqualTo(2);
		assertThat(saved.getCategoryId()).isEqualTo(102L);
		assertThat(saved.getFirstName()).isEqualTo("Bruno");
		assertThat(saved.getLastName()).isEqualTo("Torres");
		assertThat(saved.isEnabled()).isFalse();
		assertThat(saved.getCreatedAt()).isEqualTo(CREATED_AT);
		assertThat(saved.getUpdatedAt()).isEqualTo(UPDATED_AT);
	}

	@Test
	void replacesAProfessionalThatIsAlreadyStored() {
		adapter.saveAll(List.of(professional(201L, 101L, "Ana", true)));
		readFromDatabase();

		// La cátedra lo cambió de categoría y lo dio de baja.
		adapter.saveAll(List.of(professional(201L, 102L, "Ana", false)));
		readFromDatabase();

		ProfessionalEntity saved = jpaProfessionalRepository.findById(201L).orElseThrow();
		assertThat(jpaProfessionalRepository.count()).isEqualTo(1);
		assertThat(saved.getCategoryId()).isEqualTo(102L);
		assertThat(saved.isEnabled()).isFalse();
	}

	@Test
	void deletesAllProfessionals() {
		adapter.saveAll(List.of(professional(201L, 101L, "Ana", true), professional(202L, 102L, "Bruno", true)));
		readFromDatabase();

		adapter.deleteAll();

		assertThat(jpaProfessionalRepository.count()).isZero();
	}

	@Test
	void savesTheSameIdsAgainAfterDeletingAllInTheSameTransaction() {
		adapter.saveAll(List.of(professional(201L, 101L, "Ana", true)));
		readFromDatabase();

		adapter.deleteAll();
		adapter.saveAll(List.of(professional(201L, 101L, "Ana", false)));
		readFromDatabase();

		assertThat(jpaProfessionalRepository.findById(201L).orElseThrow().isEnabled()).isFalse();
	}

	// --- Búsqueda ---

	@Test
	void searchesByCategory() {
		storeForSearch();

		assertThat(idsOf(search(ProfessionalSearchCriteria.builder().categoryId(102L).build())))
				.containsExactly(203L, 204L);
	}

	@Test
	void findsAPartialMatchOnTheFullNameWithoutCaseOrAccents() {
		storeForSearch();

		// "Ana Gómez" se encuentra escrito sin tilde y en minúsculas, cruzando nombre y apellido.
		assertThat(idsOf(search(byName("ana gom")))).containsExactly(201L);
		assertThat(idsOf(search(byName("GOMEZ")))).containsExactly(201L);
	}

	@Test
	void findsANameWithoutAccentsWhenTheSearchHasThem() {
		storeForSearch();

		// Al revés que el anterior: el dato no tiene tilde y la búsqueda sí.
		assertThat(idsOf(search(byName("Martín")))).containsExactly(202L);
	}

	@Test
	void matchesTheFirstNameOrTheLastNameAlone() {
		storeForSearch();

		assertThat(idsOf(search(byName("lucia")))).containsExactly(203L);
		assertThat(idsOf(search(byName("vega")))).containsExactly(204L);
	}

	@Test
	void takesLikeWildcardsInTheNameLiterally() {
		storeForSearch();

		// Sin escape, % y _ coincidirían con cualquier nombre.
		assertThat(search(byName("%")).getContent()).isEmpty();
		assertThat(search(byName("_")).getContent()).isEmpty();
	}

	@Test
	void combinesTheCategoryAndTheName() {
		storeForSearch();

		ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder().categoryId(101L).name("a").build();
		assertThat(idsOf(search(criteria))).containsExactly(201L, 202L);
		criteria = ProfessionalSearchCriteria.builder().categoryId(102L).name("gomez").build();
		assertThat(search(criteria).getContent()).isEmpty();
	}

	@Test
	void returnsTheRequestedPageAndTheTotal() {
		storeForSearch();

		PageResult<Professional> secondPage = adapter.search(ProfessionalSearchCriteria.builder().build(), BY_LAST_NAME,
				PageQuery.builder().page(1).size(3).build());

		// Por apellido: Gómez, Paz, Rivas | Vega.
		assertThat(idsOf(secondPage)).containsExactly(204L);
		assertThat(secondPage.getTotalElements()).isEqualTo(4);
		assertThat(secondPage.getPage()).isEqualTo(1);
		assertThat(secondPage.getSize()).isEqualTo(3);
	}

	@Test
	void ordersInTheRequestedDirection() {
		storeForSearch();

		PageResult<Professional> result = adapter.search(ProfessionalSearchCriteria.builder().build(),
				List.of(sortBy(ProfessionalSortField.FIRST_NAME, true)), FIRST_PAGE);

		// Por nombre, de la Z a la A: Nicolas, Martin, Lucia, Ana.
		assertThat(idsOf(result)).containsExactly(204L, 202L, 203L, 201L);
	}

	@Test
	void breaksTiesByIdSoPagesDoNotRepeatOrSkipRows() {
		adapter.saveAll(List.of(professional(207L, 101L, "Ana", "Torres", true),
				professional(205L, 101L, "Ana", "Torres", true), professional(206L, 101L, "Ana", "Torres", true)));
		readFromDatabase();

		PageResult<Professional> first = adapter.search(ProfessionalSearchCriteria.builder().build(), BY_LAST_NAME,
				PageQuery.builder().page(0).size(2).build());
		PageResult<Professional> second = adapter.search(ProfessionalSearchCriteria.builder().build(), BY_LAST_NAME,
				PageQuery.builder().page(1).size(2).build());

		assertThat(idsOf(first)).containsExactly(205L, 206L);
		assertThat(idsOf(second)).containsExactly(207L);
	}

	@Test
	void returnsAnEmptyPageWhenNothingMatches() {
		storeForSearch();

		PageResult<Professional> result = search(byName("zzz"));

		assertThat(result.getContent()).isEmpty();
		assertThat(result.getTotalElements()).isZero();
	}

	// --- Habilitado efectivo (ADR-0014) ---

	@Test
	void enabledReturnsOnlyProfessionalsEnabledInAnEnabledCategory() {
		storeForEnabledFilter();

		assertThat(idsOf(search(byEnabled(true, Set.of(101L, 102L))))).containsExactly(201L);
	}

	@Test
	void disabledReturnsTheDisabledOnesAndThoseOfADisabledCategory() {
		storeForEnabledFilter();

		// Torres está deshabilitado; Vega está habilitado, pero su categoría no.
		assertThat(idsOf(search(byEnabled(false, Set.of(101L, 102L))))).containsExactly(202L, 203L);
	}

	@Test
	void withoutEnabledCategoriesNoProfessionalIsEnabled() {
		storeForEnabledFilter();

		assertThat(search(byEnabled(true, Set.of())).getContent()).isEmpty();
		assertThat(idsOf(search(byEnabled(false, Set.of())))).containsExactly(201L, 202L, 203L);
	}

	@Test
	void combinesTheEnabledFilterWithTheName() {
		storeForEnabledFilter();

		ProfessionalSearchCriteria criteria = byEnabled(false, Set.of(101L, 102L)).toBuilder().name("vega").build();
		assertThat(idsOf(search(criteria))).containsExactly(203L);
	}

	private void storeForEnabledFilter() {
		adapter.saveAll(List.of(
				professional(201L, 101L, "Ana", "Gómez", true),
				professional(202L, 101L, "Bruno", "Torres", false),
				professional(203L, 103L, "Nicolas", "Vega", true)));
		readFromDatabase();
	}

	private ProfessionalSearchCriteria byEnabled(boolean enabled, Set<Long> enabledCategoryIds) {
		return ProfessionalSearchCriteria.builder().enabled(enabled).enabledCategoryIds(enabledCategoryIds).build();
	}

	// --- Disponibilidad de agenda (ADR-0013). El 2026-10-26 es lunes ---

	private static final LocalDate MONDAY = LocalDate.parse("2026-10-26");

	@Test
	void dateReturnsProfessionalsWithAnEnabledScheduleThatDayOfTheWeek() {
		storeForAvailability();

		// Paz atiende los martes; Vega, los lunes, pero ese horario está deshabilitado (ADR-0014).
		assertThat(idsOf(search(byAvailability(MONDAY, null, null)))).containsExactly(201L, 202L);
	}

	@Test
	void aProfessionalWithSeveralSchedulesThatDayAppearsOnce() {
		storeForAvailability();

		PageResult<Professional> result = search(byAvailability(MONDAY, null, null));

		assertThat(result.getTotalElements()).isEqualTo(2);
	}

	@Test
	void timeRangeReturnsSchedulesThatShareAtLeastAMinuteWithIt() {
		storeForAvailability();

		// Gómez atiende de 09 a 12 y de 15 a 16; Rivas, de 14 a 18.
		assertThat(idsOf(search(byAvailability(MONDAY, "11:00", "13:00")))).containsExactly(201L);
		assertThat(idsOf(search(byAvailability(MONDAY, "10:00", "10:30")))).containsExactly(201L);
		assertThat(idsOf(search(byAvailability(MONDAY, "08:00", "20:00")))).containsExactly(201L, 202L);
		assertThat(idsOf(search(byAvailability(MONDAY, "15:30", "17:00")))).containsExactly(201L, 202L);
	}

	@Test
	void schedulesThatOnlyTouchTheBorderOfTheRangeDoNotCount() {
		storeForAvailability();

		// De 12 a 14 no queda ningún minuto: Gómez termina a las 12 y Rivas empieza a las 14.
		assertThat(search(byAvailability(MONDAY, "12:00", "14:00")).getContent()).isEmpty();
	}

	@Test
	void combinesAvailabilityWithTheOtherFilters() {
		storeForAvailability();

		ProfessionalSearchCriteria criteria = byAvailability(MONDAY, "08:00", "20:00").toBuilder()
				.name("rivas").enabled(true).enabledCategoryIds(Set.of(101L, 102L)).build();
		assertThat(idsOf(search(criteria))).containsExactly(202L);
	}

	private void storeForAvailability() {
		storeForSearch();
		entityManager.getEntityManager().createNativeQuery("""
				INSERT INTO weekly_schedule (id, professional_id, day_of_week, start_time, end_time,
					slot_duration_minutes, enabled, created_at, updated_at)
				VALUES (301, 201, 'MONDAY', '09:00', '12:00', 30, true, now(), now()),
					(302, 201, 'MONDAY', '15:00', '16:00', 20, true, now(), now()),
					(303, 202, 'MONDAY', '14:00', '18:00', 45, true, now(), now()),
					(304, 203, 'TUESDAY', '09:00', '12:00', 30, true, now(), now()),
					(305, 204, 'MONDAY', '09:00', '12:00', 30, false, now(), now())
				""").executeUpdate();
		readFromDatabase();
	}

	private ProfessionalSearchCriteria byAvailability(LocalDate date, String from, String to) {
		return ProfessionalSearchCriteria.builder()
				.date(date)
				.timeFrom(from != null ? LocalTime.parse(from) : null)
				.timeTo(to != null ? LocalTime.parse(to) : null)
				.build();
	}

	private void storeForSearch() {
		adapter.saveAll(List.of(
				professional(201L, 101L, "Ana", "Gómez", true),
				professional(202L, 101L, "Martin", "Rivas", true),
				professional(203L, 102L, "Lucía", "Paz", true),
				professional(204L, 102L, "Nicolas", "Vega", true)));
		readFromDatabase();
	}

	private PageResult<Professional> search(ProfessionalSearchCriteria criteria) {
		return adapter.search(criteria, BY_LAST_NAME, FIRST_PAGE);
	}

	private ProfessionalSearchCriteria byName(String name) {
		return ProfessionalSearchCriteria.builder().name(name).build();
	}

	private List<Long> idsOf(PageResult<Professional> page) {
		return page.getContent().stream().map(Professional::getId).toList();
	}

	private static ProfessionalSortOrder sortBy(ProfessionalSortField field, boolean descending) {
		return ProfessionalSortOrder.builder().field(field).descending(descending).build();
	}

	private Professional professional(Long id, Long categoryId, String firstName, boolean enabled) {
		return professional(id, categoryId, firstName, "Torres", enabled);
	}

	private Professional professional(Long id, Long categoryId, String firstName, String lastName, boolean enabled) {
		return Professional.builder()
				.id(id)
				.categoryId(categoryId)
				.firstName(firstName)
				.lastName(lastName)
				.enabled(enabled)
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
