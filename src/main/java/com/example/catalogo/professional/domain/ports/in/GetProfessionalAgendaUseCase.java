package com.example.catalogo.professional.domain.ports.in;

import java.util.Optional;

import com.example.catalogo.professional.domain.model.ProfessionalAgenda;

public interface GetProfessionalAgendaUseCase {

	Optional<ProfessionalAgenda> getProfessionalAgenda(Long professionalId);

}
