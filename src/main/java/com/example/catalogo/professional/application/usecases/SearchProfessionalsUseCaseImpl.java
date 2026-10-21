package com.example.catalogo.professional.application.usecases;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import com.example.catalogo.professional.application.exception.DateOutOfRangeException;
import com.example.catalogo.professional.domain.model.AgendaZone;
import com.example.catalogo.professional.domain.model.AvailabilityHorizon;
import com.example.catalogo.professional.domain.model.PageQuery;
import com.example.catalogo.professional.domain.model.PageResult;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.ProfessionalSearchCriteria;
import com.example.catalogo.professional.domain.model.ProfessionalSortField;
import com.example.catalogo.professional.domain.model.ProfessionalSortOrder;
import com.example.catalogo.professional.domain.model.ProfessionalSummary;
import com.example.catalogo.professional.domain.ports.in.SearchProfessionalsUseCase;
import com.example.catalogo.professional.domain.ports.out.ProfessionalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SearchProfessionalsUseCaseImpl implements SearchProfessionalsUseCase {

	// Sin un orden pedido, por apellido y nombre (HU-05, criterio 6).
	private static final List<ProfessionalSortOrder> DEFAULT_SORT = List.of(
			ProfessionalSortOrder.builder().field(ProfessionalSortField.LAST_NAME).build(),
			ProfessionalSortOrder.builder().field(ProfessionalSortField.FIRST_NAME).build());

	private final ProfessionalRepository professionalRepository;
	private final CategoryRepository categoryRepository;
	private final AvailabilityHorizon availabilityHorizon;
	private final Clock clock;

	// Se responde con la copia local, sin consultar a la cátedra (ENUNCIADO §6.2). Son varias
	// lecturas (la página, el total y las categorías): con REPEATABLE READ todas ven la misma
	// versión del catálogo, aunque un snapshot se aplique en el medio (ADR-0010).
	@Override
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public PageResult<ProfessionalSummary> searchProfessionals(ProfessionalSearchCriteria criteria,
			List<ProfessionalSortOrder> sort, PageQuery page) {
		checkDateInRange(criteria.getDate());

		// La categoría es de otra feature: se lee con su puerto de salida (ADR-0064) y no con un JOIN
		// a su tabla. Son pocas, y sirven para el filtro de habilitado y para completar cada resultado.
		Map<Long, Category> categories = categoryRepository.findAll().stream()
				.collect(Collectors.toMap(Category::getId, Function.identity()));
		Set<Long> enabledCategoryIds = categories.values().stream()
				.filter(Category::isEnabled)
				.map(Category::getId)
				.collect(Collectors.toSet());

		ProfessionalSearchCriteria normalized = criteria.toBuilder()
				.name(normalizeName(criteria.getName()))
				// Sin pedir el estado, solo los habilitados (ADR-0014).
				.enabled(criteria.getEnabled() != null ? criteria.getEnabled() : Boolean.TRUE)
				.enabledCategoryIds(enabledCategoryIds)
				.build();
		PageResult<Professional> professionals = professionalRepository.search(
				normalized, sort.isEmpty() ? DEFAULT_SORT : sort, page);
		return professionals.map(professional -> toSummary(professional, categories.get(professional.getCategoryId())));
	}

	// Solo fechas entre hoy y el horizonte (ADR-0065), las mismas que acepta la disponibilidad de
	// turnos. "Hoy" es el de Argentina (ADR-0016): a las 22 h del día 10 en UTC ya es 11, pero acá no.
	private void checkDateInRange(LocalDate date) {
		if (date == null) {
			return;
		}
		LocalDate today = LocalDate.now(clock.withZone(AgendaZone.ZONE));
		LocalDate last = today.plusDays(availabilityHorizon.getDays());
		if (date.isBefore(today) || date.isAfter(last)) {
			throw new DateOutOfRangeException(date, today, last);
		}
	}

	// Los espacios de más no cambian lo que se busca: "  ana   gom " es lo mismo que "ana gom".
	private String normalizeName(String name) {
		if (name == null) {
			return null;
		}
		return name.strip().replaceAll("\\s+", " ");
	}

	private ProfessionalSummary toSummary(Professional professional, Category category) {
		return ProfessionalSummary.builder()
				.id(professional.getId())
				.firstName(professional.getFirstName())
				.lastName(professional.getLastName())
				.category(category)
				.enabled(professional.isEffectivelyEnabled(category))
				.build();
	}

}
