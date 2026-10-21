package com.example.catalogo.category.domain.ports.out;

import java.util.List;

import com.example.catalogo.category.domain.model.Category;

public interface CategoryRepository {

	// La regla (solo habilitadas, por nombre) está dicha acá, en el dominio: el caso de uso elige
	// esta operación y el adaptador solo la ejecuta. Que la resuelva la base, y no el caso de uso
	// en memoria, es para traer únicamente las filas que se van a usar.
	List<Category> findAllEnabledOrderedByName();

	// Todas, habilitadas o no. La usa la búsqueda de profesionales para completar la categoría de
	// cada resultado (ADR-0064).
	List<Category> findAll();

	// Guarda por el id de la cátedra: crea las que no están y reemplaza las que ya están. La usa
	// la sincronización para escribir la copia local (ADR-0062).
	void saveAll(List<Category> categories);

	// Borra la copia local entera, para reemplazarla desde un snapshot. Los profesionales se borran antes.
	void deleteAll();

}
