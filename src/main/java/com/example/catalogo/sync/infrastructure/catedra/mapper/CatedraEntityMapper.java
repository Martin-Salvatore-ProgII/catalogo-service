package com.example.catalogo.sync.infrastructure.catedra.mapper;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.sync.infrastructure.catedra.dto.CatedraCategoryDto;
import com.example.catalogo.sync.infrastructure.catedra.dto.CatedraProfessionalDto;
import com.example.catalogo.sync.infrastructure.catedra.dto.CatedraWeeklyScheduleDto;
import org.springframework.stereotype.Component;

@Component
public class CatedraEntityMapper {

	public Category toCategory(CatedraCategoryDto dto) {
		if (dto == null) {
			return null;
		}
		return Category.builder()
				.id(dto.getId())
				.name(dto.getName())
				.description(dto.getDescription())
				.enabled(dto.isEnabled())
				.createdAt(dto.getCreatedAt())
				.updatedAt(dto.getUpdatedAt())
				.build();
	}

	public Professional toProfessional(CatedraProfessionalDto dto) {
		if (dto == null) {
			return null;
		}
		return Professional.builder()
				.id(dto.getId())
				.categoryId(dto.getCategoryId())
				.firstName(dto.getFirstName())
				.lastName(dto.getLastName())
				.enabled(dto.isEnabled())
				.createdAt(dto.getCreatedAt())
				.updatedAt(dto.getUpdatedAt())
				.build();
	}

	public WeeklySchedule toWeeklySchedule(CatedraWeeklyScheduleDto dto) {
		if (dto == null) {
			return null;
		}
		return WeeklySchedule.builder()
				.id(dto.getId())
				.professionalId(dto.getProfessionalId())
				.dayOfWeek(dto.getDayOfWeek())
				.startTime(dto.getStartTime())
				.endTime(dto.getEndTime())
				.slotDurationMinutes(dto.getSlotDurationMinutes())
				.enabled(dto.isEnabled())
				.createdAt(dto.getCreatedAt())
				.updatedAt(dto.getUpdatedAt())
				.build();
	}

}
