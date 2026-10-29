package com.example.catalogo.professional.domain.model;

import java.util.List;

import com.example.catalogo.category.domain.model.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// La agenda vigente de un profesional (ADR-0017): sus datos, su categoría, si está habilitado, sus
// horarios habilitados y la versión del catálogo con la que se armó. La usan la app y turnos.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalAgenda {
	private Long id;
	private String firstName;
	private String lastName;
	private Category category;
	// Habilitado efectivo (ADR-0014). Un profesional deshabilitado se devuelve igual: qué hacer con
	// él lo decide quien consulta.
	private boolean enabled;
	private List<WeeklySchedule> weeklySchedules;
	private long catalogVersion;
}
