package com.example.catalogo.category.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.in.GetAllCategoriesUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	@Mock
	private GetAllCategoriesUseCase getAllCategoriesUseCase;

	@InjectMocks
	private CategoryService categoryService;

	@Test
	void getAllCategoriesDelegatesToTheUseCase() {
		List<Category> categories = List.of(Category.builder().id(101L).name("Cardiologia").build());
		when(getAllCategoriesUseCase.getAllCategories()).thenReturn(categories);

		assertThat(categoryService.getAllCategories()).isSameAs(categories);
	}

}
