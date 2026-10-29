package com.example.catalogo.category.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import com.example.catalogo.category.infrastructure.persistence.mapper.CategoryMapper;
import com.example.catalogo.category.infrastructure.persistence.repository.JpaCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaCategoryRepositoryAdapter implements CategoryRepository {

	private final JpaCategoryRepository jpaCategoryRepository;
	private final CategoryMapper categoryMapper;

	@Override
	public List<Category> findAllEnabledOrderedByName() {
		return jpaCategoryRepository.findAllByEnabledTrueOrderByNameAsc().stream()
				.map(categoryMapper::toDomainModel)
				.toList();
	}

	@Override
	public Optional<Category> findById(Long id) {
		return jpaCategoryRepository.findById(id).map(categoryMapper::toDomainModel);
	}

	@Override
	public List<Category> findAll() {
		return jpaCategoryRepository.findAll().stream()
				.map(categoryMapper::toDomainModel)
				.toList();
	}

	// Con el id ya asignado, Spring Data busca la fila por id y la actualiza, o la crea si no está.
	@Override
	public void saveAll(List<Category> categories) {
		jpaCategoryRepository.saveAll(categories.stream().map(categoryMapper::toEntity).toList());
	}

	// Un solo DELETE que se ejecuta en el momento. Un borrado por entity, en cambio, Hibernate lo
	// deja para después de los INSERT, y volver a guardar los mismos ids chocaría con las filas viejas.
	@Override
	public void deleteAll() {
		jpaCategoryRepository.deleteAllInBatch();
	}

}
