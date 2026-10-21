package com.example.catalogo.professional.domain.ports.in;

import java.util.List;

import com.example.catalogo.professional.domain.model.PageQuery;
import com.example.catalogo.professional.domain.model.PageResult;
import com.example.catalogo.professional.domain.model.ProfessionalSearchCriteria;
import com.example.catalogo.professional.domain.model.ProfessionalSortOrder;
import com.example.catalogo.professional.domain.model.ProfessionalSummary;

public interface SearchProfessionalsUseCase {

	PageResult<ProfessionalSummary> searchProfessionals(ProfessionalSearchCriteria criteria,
			List<ProfessionalSortOrder> sort, PageQuery page);

}
