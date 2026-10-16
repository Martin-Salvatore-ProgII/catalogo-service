package com.example.catalogo.sync.domain.model;

import java.util.List;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Catálogo completo de la cátedra en una versión (REF §7), con las entidades habilitadas y las
// deshabilitadas. Usa los modelos de dominio de cada feature, que son los que reciben sus
// repositorios (ADR-0062).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogSnapshot {
	private long snapshotVersion;
	private List<Category> categories;
	private List<Professional> professionals;
	private List<WeeklySchedule> weeklySchedules;
}
