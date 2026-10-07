package com.example.catalogo.category.domain.ports.out;

import java.util.List;

import com.example.catalogo.category.domain.model.Category;

public interface CategoryRepository {

	// La regla (solo habilitadas, por nombre) está dicha acá, en el dominio: el caso de uso elige
	// esta operación y el adaptador solo la ejecuta. Que la resuelva la base, y no el caso de uso
	// en memoria, es para traer únicamente las filas que se van a usar.
	List<Category> findAllEnabledOrderedByName();

}
