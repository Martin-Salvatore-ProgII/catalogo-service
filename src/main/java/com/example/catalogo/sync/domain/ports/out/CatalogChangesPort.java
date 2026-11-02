package com.example.catalogo.sync.domain.ports.out;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.sync.domain.model.VersionChanges;

// Lo que hace falta para aplicar una versión: qué cambió y cómo está cada entidad ahora (REF §14.4,
// §14.5). Si la cátedra no responde, los adaptadores lanzan CatalogChangesException.
//
// Lo que falta o no se puede leer no es un error: no aparece en el resultado. Quien lo llama decide
// qué hacer (la tabla de situaciones de docs/arq/sincronizacion.md manda a un snapshot).
public interface CatalogChangesPort {

	// Vacío si la cátedra no tiene los cambios de esa versión, o si no se pueden leer.
	Optional<VersionChanges> findChanges(long version);

	// Estado actual de cada id, en una sola lectura. Los ids que no están, o no se pueden leer, no
	// aparecen en la lista. El orden no está garantizado.
	List<Category> findCategories(Collection<Long> ids);

	List<Professional> findProfessionals(Collection<Long> ids);

	List<WeeklySchedule> findWeeklySchedules(Collection<Long> ids);

}
