package com.example.catalogo.professional.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import com.example.catalogo.TestcontainersConfiguration;
import com.example.catalogo.professional.domain.model.Professional;
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
				VALUES (101, 'Cardiologia', true, now(), now()), (102, 'Pediatria', true, now(), now())
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

	private Professional professional(Long id, Long categoryId, String firstName, boolean enabled) {
		return Professional.builder()
				.id(id)
				.categoryId(categoryId)
				.firstName(firstName)
				.lastName("Torres")
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
