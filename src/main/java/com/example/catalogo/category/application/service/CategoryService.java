package com.example.catalogo.category.application.service;

import java.util.List;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.in.GetAllCategoriesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final GetAllCategoriesUseCase getAllCategoriesUseCase;

	public List<Category> getAllCategories() {
		return getAllCategoriesUseCase.getAllCategories();
	}

}
