package com.example.catalogo.professional.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.catalogo.category.domain.model.Category;
import org.junit.jupiter.api.Test;

// La regla de habilitado efectivo (ADR-0014), en sus cuatro combinaciones y sin categoría.
class ProfessionalTest {

	@Test
	void isEnabledWhenItAndItsCategoryAreEnabled() {
		assertThat(professional(true).isEffectivelyEnabled(category(true))).isTrue();
	}

	@Test
	void isNotEnabledWhenItIsDisabled() {
		assertThat(professional(false).isEffectivelyEnabled(category(true))).isFalse();
	}

	@Test
	void isNotEnabledWhenItsCategoryIsDisabled() {
		assertThat(professional(true).isEffectivelyEnabled(category(false))).isFalse();
	}

	@Test
	void isNotEnabledWhenBothAreDisabled() {
		assertThat(professional(false).isEffectivelyEnabled(category(false))).isFalse();
	}

	@Test
	void isNotEnabledWithoutAKnownCategory() {
		assertThat(professional(true).isEffectivelyEnabled(null)).isFalse();
	}

	private Professional professional(boolean enabled) {
		return Professional.builder().id(201L).categoryId(101L).enabled(enabled).build();
	}

	private Category category(boolean enabled) {
		return Category.builder().id(101L).enabled(enabled).build();
	}

}
