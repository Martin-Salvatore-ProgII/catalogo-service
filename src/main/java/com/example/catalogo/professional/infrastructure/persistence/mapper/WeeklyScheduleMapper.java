package com.example.catalogo.professional.infrastructure.persistence.mapper;

import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.professional.infrastructure.persistence.entity.WeeklyScheduleEntity;
import org.springframework.stereotype.Component;

@Component
public class WeeklyScheduleMapper {

	public WeeklyScheduleEntity toEntity(WeeklySchedule schedule) {
		if (schedule == null) {
			return null;
		}
		return WeeklyScheduleEntity.builder()
				.id(schedule.getId())
				.professionalId(schedule.getProfessionalId())
				.dayOfWeek(schedule.getDayOfWeek())
				.startTime(schedule.getStartTime())
				.endTime(schedule.getEndTime())
				.slotDurationMinutes(schedule.getSlotDurationMinutes())
				.enabled(schedule.isEnabled())
				.createdAt(schedule.getCreatedAt())
				.updatedAt(schedule.getUpdatedAt())
				.build();
	}

	public WeeklySchedule toDomainModel(WeeklyScheduleEntity entity) {
		if (entity == null) {
			return null;
		}
		return WeeklySchedule.builder()
				.id(entity.getId())
				.professionalId(entity.getProfessionalId())
				.dayOfWeek(entity.getDayOfWeek())
				.startTime(entity.getStartTime())
				.endTime(entity.getEndTime())
				.slotDurationMinutes(entity.getSlotDurationMinutes())
				.enabled(entity.isEnabled())
				.createdAt(entity.getCreatedAt())
				.updatedAt(entity.getUpdatedAt())
				.build();
	}

}
