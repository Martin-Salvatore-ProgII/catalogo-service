package com.example.catalogo.category.infrastructure.web.mapper;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.infrastructure.web.dto.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryDtoMapper {

	public CategoryResponse toResponse(Category category) {
		if (category == null) {
			return null;
		}
		return CategoryResponse.builder()
				.id(category.getId())
				.name(category.getName())
				.description(category.getDescription())
				.build();
	}

}
