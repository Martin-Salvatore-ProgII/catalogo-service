package com.example.catalogo.professional.application.usecases;

import java.util.Optional;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.ProfessionalAgenda;
import com.example.catalogo.professional.domain.ports.in.GetProfessionalAgendaUseCase;
import com.example.catalogo.professional.domain.ports.out.ProfessionalRepository;
import com.example.catalogo.professional.domain.ports.out.WeeklyScheduleRepository;
import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GetProfessionalAgendaUseCaseImpl implements GetProfessionalAgendaUseCase {

	private final ProfessionalRepository professionalRepository;
	private final WeeklyScheduleRepository weeklyScheduleRepository;
	private final CategoryRepository categoryRepository;
	private final SyncStateRepository syncStateRepository;

	// Una sola lectura consistente (ADR-0017): el profesional, su categoría, sus horarios y la versión
	// se leen en una transacción REPEATABLE READ. Como el snapshot cambia los datos y la versión juntos
	// (ADR-0010), nunca se responde con datos de una versión y el número de otra.
	@Override
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public Optional<ProfessionalAgenda> getProfessionalAgenda(Long professionalId) {
		return professionalRepository.findById(professionalId).map(this::toAgenda);
	}

	// La categoría y la versión son de otras features: se leen con sus puertos de salida, que no
	// tienen reglas propias sobre esas lecturas (ADR-0062, ADR-0064).
	private ProfessionalAgenda toAgenda(Professional professional) {
		Category category = categoryRepository.findById(professional.getCategoryId()).orElse(null);
		return ProfessionalAgenda.builder()
				.id(professional.getId())
				.firstName(professional.getFirstName())
				.lastName(professional.getLastName())
				.category(category)
				.enabled(professional.isEffectivelyEnabled(category))
				.weeklySchedules(weeklyScheduleRepository.findEnabledByProfessionalOrderedByDayAndTime(professional.getId()))
				.catalogVersion(syncStateRepository.getCurrent().getLocalVersion())
				.build();
	}

}
