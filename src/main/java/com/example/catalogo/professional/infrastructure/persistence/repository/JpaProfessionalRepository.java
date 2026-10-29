package com.example.catalogo.professional.infrastructure.persistence.repository;

import com.example.catalogo.professional.infrastructure.persistence.entity.ProfessionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaProfessionalRepository extends JpaRepository<ProfessionalEntity, Long> {
}
