package com.example.catalogo.category.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import com.example.catalogo.category.infrastructure.persistence.entity.CategoryEntity;
import com.example.catalogo.category.infrastructure.persistence.mapper.CategoryMapper;
import com.example.catalogo.category.infrastructure.persistence.repository.JpaCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component  // el @Repository para Spring es JpaCategoryRepository ; no este adaptador
@RequiredArgsConstructor
//una clase que implementa un puerto de salida es un adaptador ("adapta" la tecnologia al puerto)
// la tecnologia adaptada por este puerto es spring-data-JPA implementada en JpaCategoryRepository (no Adapter)
public class JpaCategoryRepositoryAdapter implements CategoryRepository {

	private final JpaCategoryRepository jpaCategoryRepository;
	private final CategoryMapper categoryMapper;

	@Override
	public List<Category> findAllEnabledOrderedByName() {
		return jpaCategoryRepository.findAllByEnabledTrueOrderByNameAsc().stream()
				.map(categoryMapper::toDomainModel)      // :: son formas de referenciar metodos
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

	@Override
	public Set<Long> findExistingIds(Collection<Long> ids) {
		return jpaCategoryRepository.findAllById(ids).stream().map(CategoryEntity::getId).collect(Collectors.toSet());
	}

}
