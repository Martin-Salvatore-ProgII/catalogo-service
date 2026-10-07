package com.example.catalogo.category.application.usecases;

import java.util.List;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.in.GetAllCategoriesUseCase;
import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetAllCategoriesUseCaseImpl implements GetAllCategoriesUseCase {

	private final CategoryRepository repository;

	// Las categorías se ofrecen como filtro de búsqueda: una deshabilitada no tiene que poder
	// elegirse, y se muestran por nombre. Se responde con la copia local, sin consultar a la
	// cátedra (ENUNCIADO §4.1).
	@Override
	public List<Category> getAllCategories() {
		return repository.findAllEnabledOrderedByName();
	}

}
