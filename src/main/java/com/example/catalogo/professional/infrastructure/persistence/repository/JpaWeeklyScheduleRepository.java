package com.example.catalogo.professional.infrastructure.persistence.repository;

import java.util.List;

import com.example.catalogo.professional.infrastructure.persistence.entity.WeeklyScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaWeeklyScheduleRepository extends JpaRepository<WeeklyScheduleEntity, Long> {

	List<WeeklyScheduleEntity> findAllByProfessionalIdAndEnabledTrue(Long professionalId);
}
