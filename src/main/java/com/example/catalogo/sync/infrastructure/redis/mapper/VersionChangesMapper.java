package com.example.catalogo.sync.infrastructure.redis.mapper;

import com.example.catalogo.sync.domain.model.VersionChanges;
import com.example.catalogo.sync.infrastructure.redis.dto.VersionChangesResponse;
import org.springframework.stereotype.Component;

@Component
public class VersionChangesMapper {

	// Se asume que la respuesta está completa: el adaptador ya descartó las que no lo están.
	public VersionChanges toDomainModel(VersionChangesResponse response) {
		if (response == null) {
			return null;
		}
		return VersionChanges.builder()
				.version(response.getVersion())
				.categoryIds(response.getChanges().getProfessionalCategories())
				.professionalIds(response.getChanges().getProfessionals())
				.weeklyScheduleIds(response.getChanges().getWeeklySchedules())
				.build();
	}

}
