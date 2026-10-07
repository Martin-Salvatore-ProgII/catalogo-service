package com.example.catalogo.category.infrastructure.web.controller;

import java.util.List;

import com.example.catalogo.category.application.service.CategoryService;
import com.example.catalogo.category.infrastructure.web.dto.CategoryResponse;
import com.example.catalogo.category.infrastructure.web.mapper.CategoryDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professional-categories")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService categoryService;
	private final CategoryDtoMapper categoryDtoMapper;

	@GetMapping
	public ResponseEntity<List<CategoryResponse>> getAllCategories() {
		List<CategoryResponse> categories = categoryService.getAllCategories().stream()
				.map(categoryDtoMapper::toResponse)
				.toList();
		return ResponseEntity.ok(categories);
	}

}
