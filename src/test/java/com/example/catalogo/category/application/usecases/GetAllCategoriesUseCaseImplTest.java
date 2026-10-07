package com.example.catalogo.category.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetAllCategoriesUseCaseImplTest {

	@Mock
	private CategoryRepository repository;

	@InjectMocks
	private GetAllCategoriesUseCaseImpl useCase;

	@Test
	void returnsTheEnabledCategoriesOrderedByName() {
		List<Category> categories = List.of(
				Category.builder().id(101L).name("Cardiologia").enabled(true).build(),
				Category.builder().id(102L).name("Pediatria").enabled(true).build());
		when(repository.findAllEnabledOrderedByName()).thenReturn(categories);

		assertThat(useCase.getAllCategories()).isEqualTo(categories);
	}

	@Test
	void returnsAnEmptyListWhenThereAreNoCategories() {
		when(repository.findAllEnabledOrderedByName()).thenReturn(List.of());

		assertThat(useCase.getAllCategories()).isEmpty();
	}

}
