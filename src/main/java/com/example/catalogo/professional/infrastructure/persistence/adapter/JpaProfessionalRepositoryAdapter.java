package com.example.catalogo.professional.infrastructure.persistence.adapter;

import java.util.List;

import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.ports.out.ProfessionalRepository;
import com.example.catalogo.professional.infrastructure.persistence.mapper.ProfessionalMapper;
import com.example.catalogo.professional.infrastructure.persistence.repository.JpaProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaProfessionalRepositoryAdapter implements ProfessionalRepository {

	private final JpaProfessionalRepository jpaProfessionalRepository;
	private final ProfessionalMapper professionalMapper;

	// Con el id ya asignado, Spring Data busca la fila por id y la actualiza, o la crea si no está.
	@Override
	public void saveAll(List<Professional> professionals) {
		jpaProfessionalRepository.saveAll(professionals.stream().map(professionalMapper::toEntity).toList());
	}

	// Un solo DELETE que se ejecuta en el momento. Un borrado por entity, en cambio, Hibernate lo
	// deja para después de los INSERT, y volver a guardar los mismos ids chocaría con las filas viejas.
	@Override
	public void deleteAll() {
		jpaProfessionalRepository.deleteAllInBatch();
	}

}
