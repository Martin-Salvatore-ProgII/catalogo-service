package com.example.catalogo.professional.domain.ports.out;

import java.util.List;

import com.example.catalogo.professional.domain.model.WeeklySchedule;

// Lo usa la sincronización para escribir la copia local (ADR-0062).
public interface WeeklyScheduleRepository {

	// Guarda por el id de la cátedra: crea los que no están y reemplaza los que ya están.
	void saveAll(List<WeeklySchedule> schedules);

	// Borra la copia local entera, para reemplazarla desde un snapshot.
	void deleteAll();

}
