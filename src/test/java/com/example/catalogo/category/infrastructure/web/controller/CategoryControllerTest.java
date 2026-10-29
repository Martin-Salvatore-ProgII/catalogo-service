package com.example.catalogo.category.infrastructure.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.example.catalogo.category.application.service.CategoryService;
import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.infrastructure.web.mapper.CategoryDtoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Sin los filtros de seguridad: acá se prueba el controller. Las reglas de acceso se prueban aparte.
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CategoryController.class)
@Import(CategoryDtoMapper.class)
class CategoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CategoryService categoryService;

	@Test
	void returnsTheCategoriesInTheOrderGivenByTheService() throws Exception {
		when(categoryService.getAllCategories()).thenReturn(List.of(
				category(101L, "Cardiologia", "Atencion cardiovascular"),
				category(102L, "Pediatria", "Atencion de niños")));

		mockMvc.perform(get("/api/professional-categories"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].id").value(101))
				.andExpect(jsonPath("$[0].name").value("Cardiologia"))
				.andExpect(jsonPath("$[0].description").value("Atencion cardiovascular"))
				.andExpect(jsonPath("$[1].name").value("Pediatria"));
	}

	@Test
	void exposesOnlyTheFieldsOfTheContract() throws Exception {
		when(categoryService.getAllCategories()).thenReturn(List.of(category(101L, "Cardiologia", "Atencion cardiovascular")));

		mockMvc.perform(get("/api/professional-categories"))
				.andExpect(jsonPath("$[0].enabled").doesNotExist())
				.andExpect(jsonPath("$[0].createdAt").doesNotExist())
				.andExpect(jsonPath("$[0].updatedAt").doesNotExist());
	}

	@Test
	void returnsAnEmptyArrayWhenThereAreNoCategories() throws Exception {
		when(categoryService.getAllCategories()).thenReturn(List.of());

		mockMvc.perform(get("/api/professional-categories"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	private Category category(Long id, String name, String description) {
		return Category.builder()
				.id(id)
				.name(name)
				.description(description)
				.enabled(true)
				.createdAt(Instant.parse("2026-09-01T03:00:00Z"))
				.updatedAt(Instant.parse("2026-09-01T03:00:00Z"))
				.build();
	}

}
