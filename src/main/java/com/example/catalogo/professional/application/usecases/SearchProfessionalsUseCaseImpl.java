package com.example.catalogo.professional.application.usecases;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.out.CategoryRepository;
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

	// Se responde con la copia local, sin consultar a la cátedra (ENUNCIADO §6.2). Son varias
	// lecturas (la página, el total y las categorías): con REPEATABLE READ todas ven la misma
	// versión del catálogo, aunque un snapshot se aplique en el medio (ADR-0010).
	@Override
	@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
	public PageResult<ProfessionalSummary> searchProfessionals(ProfessionalSearchCriteria criteria,
			List<ProfessionalSortOrder> sort, PageQuery page) {
		ProfessionalSearchCriteria normalized = criteria.toBuilder().name(normalizeName(criteria.getName())).build();
		PageResult<Professional> professionals = professionalRepository.search(
				normalized, sort.isEmpty() ? DEFAULT_SORT : sort, page);

		// La categoría es de otra feature: se lee con su puerto de salida (ADR-0062) y no con un JOIN
		// a su tabla. Son pocas, y así cada resultado sale con su nombre en una sola consulta más.
		Map<Long, Category> categories = categoryRepository.findAll().stream()
				.collect(Collectors.toMap(Category::getId, Function.identity()));
		return professionals.map(professional -> toSummary(professional, categories.get(professional.getCategoryId())));
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
				.build();
	}

}
