package com.example.catalogo.professional.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import com.example.catalogo.professional.domain.model.PageQuery;
import com.example.catalogo.professional.domain.model.PageResult;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.ProfessionalSearchCriteria;
import com.example.catalogo.professional.domain.model.ProfessionalSortField;
import com.example.catalogo.professional.domain.model.ProfessionalSortOrder;
import com.example.catalogo.professional.domain.model.ProfessionalSummary;
import com.example.catalogo.professional.domain.ports.out.ProfessionalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchProfessionalsUseCaseImplTest {

	private static final PageQuery FIRST_PAGE = PageQuery.builder().page(0).size(20).build();

	@Mock
	private ProfessionalRepository professionalRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@InjectMocks
	private SearchProfessionalsUseCaseImpl useCase;

	@Test
	void ordersByLastNameAndFirstNameWhenNoOrderIsGiven() {
		returnsPage(List.of());

		useCase.searchProfessionals(ProfessionalSearchCriteria.builder().build(), List.of(), FIRST_PAGE);

		List<ProfessionalSortOrder> sort = capturedSort();
		assertThat(sort).extracting(ProfessionalSortOrder::getField)
				.containsExactly(ProfessionalSortField.LAST_NAME, ProfessionalSortField.FIRST_NAME);
		assertThat(sort).noneMatch(ProfessionalSortOrder::isDescending);
	}

	@Test
	void keepsTheOrderThatWasAsked() {
		returnsPage(List.of());
		List<ProfessionalSortOrder> asked = List.of(
				ProfessionalSortOrder.builder().field(ProfessionalSortField.FIRST_NAME).descending(true).build());

		useCase.searchProfessionals(ProfessionalSearchCriteria.builder().build(), asked, FIRST_PAGE);

		assertThat(capturedSort()).isEqualTo(asked);
	}

	@Test
	void removesExtraSpacesFromTheName() {
		returnsPage(List.of());

		useCase.searchProfessionals(ProfessionalSearchCriteria.builder().name("  ana   gom ").build(), List.of(), FIRST_PAGE);

		assertThat(capturedCriteria().getName()).isEqualTo("ana gom");
	}

	@Test
	void passesTheOtherFiltersAndThePageAsTheyCame() {
		returnsPage(List.of());
		PageQuery thirdPage = PageQuery.builder().page(2).size(5).build();

		useCase.searchProfessionals(ProfessionalSearchCriteria.builder().categoryId(101L).build(), List.of(), thirdPage);

		ProfessionalSearchCriteria criteria = capturedCriteria();
		assertThat(criteria.getCategoryId()).isEqualTo(101L);
		assertThat(criteria.getName()).isNull();
		verify(professionalRepository).search(any(), any(), eq(thirdPage));
	}

	@Test
	void completesEachResultWithItsCategory() {
		returnsPage(List.of(professional(201L, 101L), professional(204L, 102L)));
		when(categoryRepository.findAll()).thenReturn(List.of(category(101L, "Cardiologia"), category(102L, "Pediatria")));

		PageResult<ProfessionalSummary> result = useCase.searchProfessionals(
				ProfessionalSearchCriteria.builder().build(), List.of(), FIRST_PAGE);

		assertThat(result.getContent()).extracting(summary -> summary.getCategory().getName())
				.containsExactly("Cardiologia", "Pediatria");
		assertThat(result.getContent().getFirst().getFirstName()).isEqualTo("Ana");
		assertThat(result.getTotalElements()).isEqualTo(2);
	}

	@Test
	void showsOnlyEnabledProfessionalsWhenTheStateIsNotAsked() {
		returnsPage(List.of());

		useCase.searchProfessionals(ProfessionalSearchCriteria.builder().build(), List.of(), FIRST_PAGE);

		assertThat(capturedCriteria().getEnabled()).isTrue();
	}

	@Test
	void keepsTheDisabledFilterWhenItIsAsked() {
		returnsPage(List.of());

		useCase.searchProfessionals(ProfessionalSearchCriteria.builder().enabled(false).build(), List.of(), FIRST_PAGE);

		assertThat(capturedCriteria().getEnabled()).isFalse();
	}

	@Test
	void passesOnlyTheEnabledCategoriesToResolveTheFilter() {
		returnsPage(List.of());
		when(categoryRepository.findAll()).thenReturn(List.of(category(101L, "Cardiologia"),
				Category.builder().id(108L).name("Salud mental").enabled(false).build()));

		useCase.searchProfessionals(ProfessionalSearchCriteria.builder().build(), List.of(), FIRST_PAGE);

		assertThat(capturedCriteria().getEnabledCategoryIds()).containsExactly(101L);
	}

	@Test
	void marksEachResultWithItsEffectiveState() {
		// Uno habilitado en una categoría habilitada; otro habilitado, pero en una categoría dada de baja.
		returnsPage(List.of(professional(201L, 101L), professional(220L, 108L)));
		when(categoryRepository.findAll()).thenReturn(List.of(category(101L, "Cardiologia"),
				Category.builder().id(108L).name("Salud mental").enabled(false).build()));

		PageResult<ProfessionalSummary> result = useCase.searchProfessionals(
				ProfessionalSearchCriteria.builder().enabled(false).build(), List.of(), FIRST_PAGE);

		assertThat(result.getContent()).extracting(ProfessionalSummary::isEnabled).containsExactly(true, false);
	}

	private void returnsPage(List<Professional> professionals) {
		when(professionalRepository.search(any(), any(), any())).thenReturn(PageResult.<Professional>builder()
				.content(professionals)
				.totalElements(professionals.size())
				.page(0)
				.size(20)
				.build());
	}

	@SuppressWarnings("unchecked")
	private List<ProfessionalSortOrder> capturedSort() {
		ArgumentCaptor<List<ProfessionalSortOrder>> captor = ArgumentCaptor.forClass(List.class);
		verify(professionalRepository).search(any(), captor.capture(), any());
		return captor.getValue();
	}

	private ProfessionalSearchCriteria capturedCriteria() {
		ArgumentCaptor<ProfessionalSearchCriteria> captor = ArgumentCaptor.forClass(ProfessionalSearchCriteria.class);
		verify(professionalRepository).search(captor.capture(), any(), any());
		return captor.getValue();
	}

	private Professional professional(Long id, Long categoryId) {
		return Professional.builder().id(id).categoryId(categoryId).firstName("Ana").lastName("Torres").enabled(true).build();
	}

	private Category category(Long id, String name) {
		return Category.builder().id(id).name(name).enabled(true).build();
	}

}
