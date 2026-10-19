package com.example.catalogo.category.infrastructure.persistence.mapper;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

	public CategoryEntity toEntity(Category category) {
		if (category == null) {
			return null;
		}
		return CategoryEntity.builder()
				.id(category.getId())
				.name(category.getName())
				.description(category.getDescription())
				.enabled(category.isEnabled())
				.createdAt(category.getCreatedAt())
				.updatedAt(category.getUpdatedAt())
				.build();
	}

	public Category toDomainModel(CategoryEntity entity) {
		if (entity == null) {
			return null;
		}
		return Category.builder()
				.id(entity.getId())
				.name(entity.getName())
				.description(entity.getDescription())
				.enabled(entity.isEnabled())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}

}
