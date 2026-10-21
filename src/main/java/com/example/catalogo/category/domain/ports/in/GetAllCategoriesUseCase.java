package com.example.catalogo.category.domain.ports.in;

import java.util.List;

import com.example.catalogo.category.domain.model.Category;

public interface GetAllCategoriesUseCase {

	List<Category> getAllCategories();

}
