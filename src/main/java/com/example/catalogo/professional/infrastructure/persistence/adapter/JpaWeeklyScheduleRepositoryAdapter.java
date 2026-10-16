package com.example.catalogo.professional.infrastructure.persistence.adapter;

import java.util.List;

import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.professional.domain.ports.out.WeeklyScheduleRepository;
import com.example.catalogo.professional.infrastructure.persistence.mapper.WeeklyScheduleMapper;
import com.example.catalogo.professional.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaWeeklyScheduleRepositoryAdapter implements WeeklyScheduleRepository {

	private final JpaWeeklyScheduleRepository jpaWeeklyScheduleRepository;
	private final WeeklyScheduleMapper weeklyScheduleMapper;

	// Con el id ya asignado, Spring Data busca la fila por id y la actualiza, o la crea si no está.
	@Override
	public void saveAll(List<WeeklySchedule> schedules) {
		jpaWeeklyScheduleRepository.saveAll(schedules.stream().map(weeklyScheduleMapper::toEntity).toList());
	}

	// Un solo DELETE inmediato, como en JpaProfessionalRepositoryAdapter.
	@Override
	public void deleteAll() {
		jpaWeeklyScheduleRepository.deleteAllInBatch();
	}

}
