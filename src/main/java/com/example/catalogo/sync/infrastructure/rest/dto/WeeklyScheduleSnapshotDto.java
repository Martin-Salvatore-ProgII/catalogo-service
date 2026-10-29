package com.example.catalogo.sync.infrastructure.rest.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeeklyScheduleSnapshotDto {
	private Long id;
	private Long professionalId;
	// Un día que no sea uno de los siete del contrato hace ilegible la respuesta entera.
	private DayOfWeek dayOfWeek;
	// LocalTime acepta "HH:mm" y "HH:mm:ss", los dos formatos que admite la REF §4.
	private LocalTime startTime;
	private LocalTime endTime;
	private int slotDurationMinutes;
	private boolean enabled;
	private Instant createdAt;
	private Instant updatedAt;
}
