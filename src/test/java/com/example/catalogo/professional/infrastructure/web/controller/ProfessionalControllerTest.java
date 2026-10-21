package com.example.catalogo.professional.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.professional.application.service.ProfessionalService;
import com.example.catalogo.professional.domain.model.PageQuery;
import com.example.catalogo.professional.domain.model.PageResult;
import com.example.catalogo.professional.domain.model.ProfessionalSearchCriteria;
import com.example.catalogo.professional.domain.model.ProfessionalSortField;
import com.example.catalogo.professional.domain.model.ProfessionalSortOrder;
import com.example.catalogo.professional.domain.model.ProfessionalSummary;
import com.example.catalogo.professional.infrastructure.web.mapper.ProfessionalDtoMapper;
import com.example.catalogo.shared.infrastructure.config.PaginationConfig;
import com.example.catalogo.sync.application.exception.CatalogNotReadyException;
import com.example.catalogo.sync.application.service.SyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// Sin los filtros de seguridad: acá se prueba el controller. Las reglas de acceso se prueban aparte.
// El tamaño de página por defecto y el máximo vienen de application.yaml: 20 y 100.
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ProfessionalController.class)
@Import({ ProfessionalDtoMapper.class, PaginationConfig.class })
class ProfessionalControllerTest {

	private static final String PROFESSIONALS = "/api/professionals";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProfessionalService professionalService;

	// La usa el interceptor de @RequiresCatalogReady. Como mock no lanza nada: el catálogo está listo.
	@MockitoBean
	private SyncService syncService;

	@BeforeEach
	void returnsAnEmptyPageByDefault() {
		returns(List.of(), 0, 0, 20);
	}

	// --- Lo que llega a la fachada ---

	@Test
	void withoutParametersAsksForTheFirstPageWithTheDefaultSizeAndNoFilters() throws Exception {
		mockMvc.perform(get(PROFESSIONALS)).andExpect(status().isOk());

		assertThat(capturedCriteria().getCategoryId()).isNull();
		assertThat(capturedCriteria().getName()).isNull();
		assertThat(capturedPage().getPage()).isZero();
		assertThat(capturedPage().getSize()).isEqualTo(20);
		// Vacío: el orden por defecto lo pone el caso de uso.
		assertThat(capturedSort()).isEmpty();
	}

	@Test
	void passesTheFiltersAndThePage() throws Exception {
		mockMvc.perform(get(PROFESSIONALS).param("categoryId", "101").param("name", "ana gom")
				.param("page", "2").param("size", "5"))
				.andExpect(status().isOk());

		assertThat(capturedCriteria().getCategoryId()).isEqualTo(101L);
		assertThat(capturedCriteria().getName()).isEqualTo("ana gom");
		assertThat(capturedPage().getPage()).isEqualTo(2);
		assertThat(capturedPage().getSize()).isEqualTo(5);
	}

	@Test
	void readsSeveralFieldsAndTheDirectionOfOneSortParameter() throws Exception {
		mockMvc.perform(get(PROFESSIONALS).param("sort", "lastName,firstName,desc")).andExpect(status().isOk());

		assertThat(capturedSort()).extracting(ProfessionalSortOrder::getField)
				.containsExactly(ProfessionalSortField.LAST_NAME, ProfessionalSortField.FIRST_NAME);
		assertThat(capturedSort()).allMatch(ProfessionalSortOrder::isDescending);
	}

	@Test
	void readsEachSortParameterWithItsOwnDirection() throws Exception {
		mockMvc.perform(get(PROFESSIONALS).param("sort", "lastName,DESC").param("sort", "id"))
				.andExpect(status().isOk());

		List<ProfessionalSortOrder> sort = capturedSort();
		assertThat(sort).extracting(ProfessionalSortOrder::getField)
				.containsExactly(ProfessionalSortField.LAST_NAME, ProfessionalSortField.ID);
		assertThat(sort).extracting(ProfessionalSortOrder::isDescending).containsExactly(true, false);
	}

	// --- La respuesta ---

	@Test
	void returnsTheProfessionalsWithTheirCategory() throws Exception {
		returns(List.of(summary(201L, "Ana", "Gómez", 101L, "Cardiologia")), 1, 0, 20);

		mockMvc.perform(get(PROFESSIONALS))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(201))
				.andExpect(jsonPath("$[0].firstName").value("Ana"))
				.andExpect(jsonPath("$[0].lastName").value("Gómez"))
				.andExpect(jsonPath("$[0].category.id").value(101))
				.andExpect(jsonPath("$[0].category.name").value("Cardiologia"))
				.andExpect(jsonPath("$[0].category.description").doesNotExist())
				.andExpect(jsonPath("$[0].categoryId").doesNotExist());
	}

	@Test
	void informsTheTotalAndTheLinksToTheOtherPages() throws Exception {
		returns(List.of(summary(201L, "Ana", "Gómez", 101L, "Cardiologia")), 45, 1, 20);

		// En la URL y no con param(): los enlaces se arman con el query string del pedido.
		mockMvc.perform(get(PROFESSIONALS + "?name=ana&page=1"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Total-Count", "45"))
				// Los enlaces repiten los filtros y cambian solo la página. 45 en páginas de 20: 0, 1 y 2.
				.andExpect(header().string("Link", "<http://localhost/api/professionals?name=ana&page=2>; rel=\"next\","
						+ "<http://localhost/api/professionals?name=ana&page=0>; rel=\"prev\","
						+ "<http://localhost/api/professionals?name=ana&page=2>; rel=\"last\","
						+ "<http://localhost/api/professionals?name=ana&page=0>; rel=\"first\""));
	}

	// --- Filtros inválidos (HU-05, criterio 8) ---

	@ParameterizedTest(name = "{0}={1}")
	@CsvSource({
			"name, '   '",
			"page, -1",
			"size, 0",
			"size, 101",
			"categoryId, abc",
			"sort, email",
			"sort, 'lastName,sideways'",
			"sort, 'desc'",
	})
	void rejectsAnInvalidParameterWithoutReachingTheService(String parameter, String value) throws Exception {
		mockMvc.perform(get(PROFESSIONALS).param(parameter, value))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value(org.hamcrest.Matchers.startsWith(parameter)));
		verifyNoInteractions(professionalService);
	}

	@Test
	void rejectsANameLongerThan100Characters() throws Exception {
		mockMvc.perform(get(PROFESSIONALS).param("name", "a".repeat(101)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void acceptsTheMaximumPageSize() throws Exception {
		mockMvc.perform(get(PROFESSIONALS).param("size", "100")).andExpect(status().isOk());
	}

	@Test
	void answersCatalogNotReadyWithoutReachingTheService() throws Exception {
		doThrow(new CatalogNotReadyException()).when(syncService).requireCatalogReady();

		mockMvc.perform(get(PROFESSIONALS))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("CATALOG_NOT_READY"));
		verifyNoInteractions(professionalService);
	}

	private void returns(List<ProfessionalSummary> content, long total, int page, int size) {
		when(professionalService.searchProfessionals(any(), any(), any())).thenReturn(PageResult.<ProfessionalSummary>builder()
				.content(content)
				.totalElements(total)
				.page(page)
				.size(size)
				.build());
	}

	private ProfessionalSearchCriteria capturedCriteria() {
		ArgumentCaptor<ProfessionalSearchCriteria> captor = ArgumentCaptor.forClass(ProfessionalSearchCriteria.class);
		verify(professionalService).searchProfessionals(captor.capture(), any(), any());
		return captor.getValue();
	}

	@SuppressWarnings("unchecked")
	private List<ProfessionalSortOrder> capturedSort() {
		ArgumentCaptor<List<ProfessionalSortOrder>> captor = ArgumentCaptor.forClass(List.class);
		verify(professionalService).searchProfessionals(any(), captor.capture(), any());
		return captor.getValue();
	}

	private PageQuery capturedPage() {
		ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
		verify(professionalService).searchProfessionals(any(), any(), captor.capture());
		return captor.getValue();
	}

	private ProfessionalSummary summary(Long id, String firstName, String lastName, Long categoryId, String categoryName) {
		return ProfessionalSummary.builder()
				.id(id)
				.firstName(firstName)
				.lastName(lastName)
				.category(Category.builder().id(categoryId).name(categoryName).description("Descripcion").enabled(true).build())
				.build();
	}

}
