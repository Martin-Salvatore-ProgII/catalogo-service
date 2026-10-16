package com.example.catalogo.professional.infrastructure.persistence.entity;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "weekly_schedule")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyScheduleEntity {
	// Sin @GeneratedValue ni auditoría automática: id y fechas son los de la cátedra.
	@Id
	private Long id;
	private Long professionalId;
	@Enumerated(EnumType.STRING)
	private DayOfWeek dayOfWeek;
	private LocalTime startTime;
	private LocalTime endTime;
	private int slotDurationMinutes;
	private boolean enabled;
	private Instant createdAt;
	private Instant updatedAt;
}
