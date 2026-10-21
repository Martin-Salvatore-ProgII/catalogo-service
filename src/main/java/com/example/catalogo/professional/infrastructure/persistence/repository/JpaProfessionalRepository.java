package com.example.catalogo.professional.infrastructure.persistence.repository;

import com.example.catalogo.professional.infrastructure.persistence.entity.ProfessionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

// JpaSpecificationExecutor permite armar la consulta de búsqueda con los filtros que vengan, en lugar
// de un método por cada combinación de filtros.
@Repository
public interface JpaProfessionalRepository extends JpaRepository<ProfessionalEntity, Long>,
		JpaSpecificationExecutor<ProfessionalEntity> {
}
