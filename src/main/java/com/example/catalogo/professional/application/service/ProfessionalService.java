package com.example.catalogo.professional.application.service;

import java.util.List;

import com.example.catalogo.professional.domain.model.PageQuery;
import com.example.catalogo.professional.domain.model.PageResult;
import com.example.catalogo.professional.domain.model.ProfessionalSearchCriteria;
import com.example.catalogo.professional.domain.model.ProfessionalSortOrder;
import com.example.catalogo.professional.domain.model.ProfessionalSummary;
import com.example.catalogo.professional.domain.ports.in.SearchProfessionalsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfessionalService {

	private final SearchProfessionalsUseCase searchProfessionalsUseCase;

	public PageResult<ProfessionalSummary> searchProfessionals(ProfessionalSearchCriteria criteria,
			List<ProfessionalSortOrder> sort, PageQuery page) {
		return searchProfessionalsUseCase.searchProfessionals(criteria, sort, page);
	}

}
