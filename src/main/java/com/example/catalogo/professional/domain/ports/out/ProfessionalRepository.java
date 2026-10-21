package com.example.catalogo.professional.domain.ports.out;

import java.util.List;

import com.example.catalogo.professional.domain.model.PageQuery;
import com.example.catalogo.professional.domain.model.PageResult;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.ProfessionalSearchCriteria;
import com.example.catalogo.professional.domain.model.ProfessionalSortOrder;

// La escritura la usa la sincronización para guardar la copia local (ADR-0062); la búsqueda, la app.
public interface ProfessionalRepository {

	// Los profesionales que cumplen todos los filtros indicados, en el orden pedido. El nombre es una
	// coincidencia parcial sobre el nombre completo ("nombre apellido") que no distingue mayúsculas
	// ni tildes (HU-05): "ana gom" encuentra a "Ana Gómez". La regla está declarada acá y la cumple
	// la base, que pagina y cuenta sin traer todas las filas a memoria.
	PageResult<Professional> search(ProfessionalSearchCriteria criteria, List<ProfessionalSortOrder> sort,
			PageQuery page);

	// Guarda por el id de la cátedra: crea los que no están y reemplaza los que ya están.
	void saveAll(List<Professional> professionals);

	// Borra la copia local entera, para reemplazarla desde un snapshot. Los horarios se borran antes.
	void deleteAll();

}
