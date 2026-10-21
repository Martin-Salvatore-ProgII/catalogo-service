package com.example.catalogo.professional.domain.ports.out;

import java.util.List;
import java.util.Optional;

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
	// El filtro enabled es el habilitado efectivo de ADR-0014, igual que Professional.isEffectivelyEnabled:
	// habilitado es enabled propio en true y la categoría entre criteria.enabledCategoryIds.
	// La fecha es disponibilidad de agenda (ADR-0013): algún horario habilitado el día de la semana de
	// esa fecha y, si hay franja, que comparta al menos un minuto con ella (tocarse en el borde no alcanza).
	Optional<Professional> findById(Long id);

	PageResult<Professional> search(ProfessionalSearchCriteria criteria, List<ProfessionalSortOrder> sort,
			PageQuery page);

	// Guarda por el id de la cátedra: crea los que no están y reemplaza los que ya están.
	void saveAll(List<Professional> professionals);

	// Borra la copia local entera, para reemplazarla desde un snapshot. Los horarios se borran antes.
	void deleteAll();

}
