package com.example.catalogo.category.infrastructure.persistence.adapter;

import java.util.List;

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

}
