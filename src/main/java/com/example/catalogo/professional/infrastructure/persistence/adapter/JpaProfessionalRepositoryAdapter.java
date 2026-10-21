package com.example.catalogo.professional.infrastructure.persistence.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.catalogo.professional.domain.model.PageQuery;
import com.example.catalogo.professional.domain.model.PageResult;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.ProfessionalSearchCriteria;
import com.example.catalogo.professional.domain.model.ProfessionalSortField;
import com.example.catalogo.professional.domain.model.ProfessionalSortOrder;
import com.example.catalogo.professional.domain.ports.out.ProfessionalRepository;
import com.example.catalogo.professional.infrastructure.persistence.entity.ProfessionalEntity;
import com.example.catalogo.professional.infrastructure.persistence.mapper.ProfessionalMapper;
import com.example.catalogo.professional.infrastructure.persistence.repository.JpaProfessionalRepository;
import com.example.catalogo.professional.infrastructure.persistence.repository.ProfessionalSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaProfessionalRepositoryAdapter implements ProfessionalRepository {

	private final JpaProfessionalRepository jpaProfessionalRepository;
	private final ProfessionalMapper professionalMapper;

	@Override
	public Optional<Professional> findById(Long id) {
		return jpaProfessionalRepository.findById(id).map(professionalMapper::toDomainModel);
	}

	@Override
	public PageResult<Professional> search(ProfessionalSearchCriteria criteria, List<ProfessionalSortOrder> sort,
			PageQuery page) {
		Specification<ProfessionalEntity> specification = Specification.unrestricted();
		if (criteria.getCategoryId() != null) {
			specification = specification.and(ProfessionalSpecifications.hasCategory(criteria.getCategoryId()));
		}
		if (criteria.getName() != null) {
			specification = specification.and(ProfessionalSpecifications.nameContains(criteria.getName()));
		}
		if (criteria.getEnabled() != null) {
			Specification<ProfessionalEntity> enabled =
					ProfessionalSpecifications.effectivelyEnabled(criteria.getEnabledCategoryIds());
			specification = specification.and(criteria.getEnabled() ? enabled : Specification.not(enabled));
		}
		if (criteria.getDate() != null) {
			specification = specification.and(ProfessionalSpecifications.hasScheduleOn(
					criteria.getDate().getDayOfWeek(), criteria.getTimeFrom(), criteria.getTimeTo()));
		}

		// Spring Data hace dos consultas: la página y el total que cumple los filtros.
		Page<ProfessionalEntity> result = jpaProfessionalRepository.findAll(
				specification, PageRequest.of(page.getPage(), page.getSize(), toSort(sort)));
		return PageResult.<Professional>builder()
				.content(result.getContent().stream().map(professionalMapper::toDomainModel).toList())
				.totalElements(result.getTotalElements())
				.page(page.getPage())
				.size(page.getSize())
				.build();
	}

	// Con dos "Ana Gomez", el orden entre ellos lo decidiría la base y podría cambiar de una página a
	// la otra: uno se repetiría y otro no aparecería. Desempatar por id deja un orden único.
	private Sort toSort(List<ProfessionalSortOrder> sort) {
		List<Sort.Order> orders = new ArrayList<>();
		for (ProfessionalSortOrder order : sort) {
			String property = toProperty(order.getField());
			orders.add(order.isDescending() ? Sort.Order.desc(property) : Sort.Order.asc(property));
		}
		if (sort.stream().noneMatch(order -> order.getField() == ProfessionalSortField.ID)) {
			orders.add(Sort.Order.asc("id"));
		}
		return Sort.by(orders);
	}

	private String toProperty(ProfessionalSortField field) {
		return switch (field) {
			case FIRST_NAME -> "firstName";
			case LAST_NAME -> "lastName";
			case ID -> "id";
		};
	}

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
