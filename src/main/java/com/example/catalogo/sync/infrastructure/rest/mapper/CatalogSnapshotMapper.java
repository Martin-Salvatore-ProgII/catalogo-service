package com.example.catalogo.sync.infrastructure.rest.mapper;

import com.example.catalogo.sync.domain.model.CatalogSnapshot;
import com.example.catalogo.sync.infrastructure.catedra.mapper.CatedraEntityMapper;
import com.example.catalogo.sync.infrastructure.rest.dto.CatalogSnapshotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// arma el CatalogSnapshot mapeando los 3 DTO a su funcion que arma sus respectivas entidades
@Component
@RequiredArgsConstructor
public class CatalogSnapshotMapper {

	private final CatedraEntityMapper entityMapper;

	public CatalogSnapshot toDomainModel(CatalogSnapshotResponse response) {
		if (response == null) {
			return null;
		}
		return CatalogSnapshot.builder()
				.snapshotVersion(response.getSnapshotVersion())
				.categories(response.getProfessionalCategories().stream().map(entityMapper::toCategory).toList())
				.professionals(response.getProfessionals().stream().map(entityMapper::toProfessional).toList())
				.weeklySchedules(response.getWeeklySchedules().stream().map(entityMapper::toWeeklySchedule).toList())
				.build();
	}

}
