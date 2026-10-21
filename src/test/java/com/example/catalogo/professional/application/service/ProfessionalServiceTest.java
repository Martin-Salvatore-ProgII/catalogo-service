package com.example.catalogo.professional.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.example.catalogo.professional.application.exception.ProfessionalNotFoundException;
import com.example.catalogo.professional.domain.model.PageQuery;
import com.example.catalogo.professional.domain.model.ProfessionalAgenda;
import com.example.catalogo.professional.domain.model.PageResult;
import com.example.catalogo.professional.domain.model.ProfessionalSearchCriteria;
import com.example.catalogo.professional.domain.model.ProfessionalSummary;
import com.example.catalogo.professional.domain.ports.in.GetProfessionalAgendaUseCase;
import com.example.catalogo.professional.domain.ports.in.SearchProfessionalsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfessionalServiceTest {

	@Mock
	private SearchProfessionalsUseCase searchProfessionalsUseCase;

	@Mock
	private GetProfessionalAgendaUseCase getProfessionalAgendaUseCase;

	@InjectMocks
	private ProfessionalService professionalService;

	@Test
	void searchProfessionalsDelegatesToTheUseCase() {
		ProfessionalSearchCriteria criteria = ProfessionalSearchCriteria.builder().name("ana").build();
		PageQuery page = PageQuery.builder().page(0).size(20).build();
		PageResult<ProfessionalSummary> result = PageResult.<ProfessionalSummary>builder().content(List.of()).build();
		when(searchProfessionalsUseCase.searchProfessionals(criteria, List.of(), page)).thenReturn(result);

		assertThat(professionalService.searchProfessionals(criteria, List.of(), page)).isSameAs(result);
	}

	@Test
	void getProfessionalAgendaReturnsTheAgendaOfTheUseCase() {
		ProfessionalAgenda agenda = ProfessionalAgenda.builder().id(201L).build();
		when(getProfessionalAgendaUseCase.getProfessionalAgenda(201L)).thenReturn(Optional.of(agenda));

		assertThat(professionalService.getProfessionalAgenda(201L)).isSameAs(agenda);
	}

	@Test
	void getProfessionalAgendaTurnsAMissingProfessionalIntoTheException() {
		when(getProfessionalAgendaUseCase.getProfessionalAgenda(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> professionalService.getProfessionalAgenda(999L))
				.isInstanceOf(ProfessionalNotFoundException.class);
	}

}
