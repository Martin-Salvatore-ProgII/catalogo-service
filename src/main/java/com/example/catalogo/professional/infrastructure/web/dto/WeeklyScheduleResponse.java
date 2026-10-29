package com.example.catalogo.professional.infrastructure.web.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un horario semanal (esquema WeeklySchedule del contrato).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyScheduleResponse {
	private Long id;
	private DayOfWeek dayOfWeek;
	// El contrato responde HH:mm:ss. Sin el formato, Jackson escribiría "09:00" cuando los segundos son cero.
	@JsonFormat(pattern = "HH:mm:ss")
	private LocalTime startTime;
	@JsonFormat(pattern = "HH:mm:ss")
	private LocalTime endTime;
	private int slotDurationMinutes;
}
