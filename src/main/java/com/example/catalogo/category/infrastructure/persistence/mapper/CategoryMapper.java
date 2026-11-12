package com.example.catalogo.category.infrastructure.persistence.mapper;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.stereotype.Component;

// quien elige cuando usar el sentido de escritura/lectura es el adaptador
@Component
public class CategoryMapper {

	//domino --> category entity    . Escribir en la DB   .  convierte objeto model a objeto entidad
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

	// category entity --> dominio     . Leer de la DB     .  convierte objeto entidad a objeto model
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
