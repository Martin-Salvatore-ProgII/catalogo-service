package com.example.catalogo.category.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import com.example.catalogo.TestcontainersConfiguration;
import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.infrastructure.persistence.entity.CategoryEntity;
import com.example.catalogo.category.infrastructure.persistence.mapper.CategoryMapper;
import com.example.catalogo.category.infrastructure.persistence.repository.JpaCategoryRepository;
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
@Import({ TestcontainersConfiguration.class, JpaCategoryRepositoryAdapter.class, CategoryMapper.class })
class JpaCategoryRepositoryAdapterTest {

	private static final Instant CREATED_AT = Instant.parse("2026-09-01T03:00:00Z");
	private static final Instant UPDATED_AT = Instant.parse("2026-09-10T13:40:00Z");

	@Autowired
	private JpaCategoryRepositoryAdapter adapter;

	@Autowired
	private JpaCategoryRepository jpaCategoryRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void returnsOnlyEnabledCategories() {
		store(101L, "Cardiologia", true);
		store(102L, "Pediatria", false);

		assertThat(adapter.findAllEnabledOrderedByName()).extracting(Category::getName).containsExactly("Cardiologia");
	}

	@Test
	void returnsCategoriesOrderedByName() {
		store(103L, "Pediatria", true);
		store(101L, "Traumatologia", true);
		store(102L, "Cardiologia", true);

		assertThat(adapter.findAllEnabledOrderedByName())
				.extracting(Category::getName)
				.containsExactly("Cardiologia", "Pediatria", "Traumatologia");
	}

	@Test
	void mapsEveryFieldOfTheCategory() {
		store(101L, "Cardiologia", true);

		Category category = adapter.findAllEnabledOrderedByName().getFirst();

		assertThat(category.getId()).isEqualTo(101L);
		assertThat(category.getName()).isEqualTo("Cardiologia");
		assertThat(category.getDescription()).isEqualTo("Descripcion de Cardiologia");
		assertThat(category.isEnabled()).isTrue();
		assertThat(category.getCreatedAt()).isEqualTo(CREATED_AT);
		assertThat(category.getUpdatedAt()).isEqualTo(UPDATED_AT);
	}

	@Test
	void returnsAnEmptyListWhenTheTableIsEmpty() {
		List<Category> categories = adapter.findAllEnabledOrderedByName();

		assertThat(categories).isEmpty();
	}

	// Guarda una fila como lo hará la sincronización, con el id de la cátedra, y vacía la memoria
	// de JPA para que la lectura salga de PostgreSQL.
	private void store(Long id, String name, boolean enabled) {
		jpaCategoryRepository.save(CategoryEntity.builder()
				.id(id)
				.name(name)
				.description("Descripcion de " + name)
				.enabled(enabled)
				.createdAt(CREATED_AT)
				.updatedAt(UPDATED_AT)
				.build());
		entityManager.flush();
		entityManager.clear();
	}

}
