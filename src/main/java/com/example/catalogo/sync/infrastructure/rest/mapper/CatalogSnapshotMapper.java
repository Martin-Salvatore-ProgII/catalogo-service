package com.example.catalogo.sync.infrastructure.rest.mapper;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.sync.domain.model.CatalogSnapshot;
import com.example.catalogo.sync.infrastructure.rest.dto.CatalogSnapshotResponse;
import com.example.catalogo.sync.infrastructure.rest.dto.CategorySnapshotDto;
import com.example.catalogo.sync.infrastructure.rest.dto.ProfessionalSnapshotDto;
import com.example.catalogo.sync.infrastructure.rest.dto.WeeklyScheduleSnapshotDto;
import org.springframework.stereotype.Component;

@Component
public class CatalogSnapshotMapper {

	public CatalogSnapshot toDomainModel(CatalogSnapshotResponse response) {
		if (response == null) {
			return null;
		}
		return CatalogSnapshot.builder()
				.snapshotVersion(response.getSnapshotVersion())
				.categories(response.getProfessionalCategories().stream().map(this::toCategory).toList())
				.professionals(response.getProfessionals().stream().map(this::toProfessional).toList())
				.weeklySchedules(response.getWeeklySchedules().stream().map(this::toWeeklySchedule).toList())
				.build();
	}

	private Category toCategory(CategorySnapshotDto dto) {
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

	private Professional toProfessional(ProfessionalSnapshotDto dto) {
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

	private WeeklySchedule toWeeklySchedule(WeeklyScheduleSnapshotDto dto) {
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
