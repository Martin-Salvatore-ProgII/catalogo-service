package com.example.catalogo.professional.domain.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Horario semanal de atención de un profesional, con los mismos datos que publica la cátedra
// (REF §7).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySchedule {
	// El id es el que asigna la cátedra, no uno propio.
	private Long id;
	private Long professionalId;
	// Los días de la cátedra son exactamente los nombres de DayOfWeek (MONDAY a SUNDAY).
	private DayOfWeek dayOfWeek;
	// Horas locales del negocio, sin zona (REF §4).
	private LocalTime startTime;
	private LocalTime endTime;
	private int slotDurationMinutes;
	private boolean enabled;
	private Instant createdAt;
	private Instant updatedAt;
}
