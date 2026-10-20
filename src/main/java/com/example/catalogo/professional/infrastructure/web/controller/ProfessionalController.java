package com.example.catalogo.professional.infrastructure.web.controller;

import java.util.List;

import com.example.catalogo.professional.application.service.ProfessionalService;
import com.example.catalogo.professional.domain.model.PageResult;
import com.example.catalogo.professional.domain.model.ProfessionalSummary;
import com.example.catalogo.professional.infrastructure.web.dto.ProfessionalSearchRequest;
import com.example.catalogo.professional.infrastructure.web.dto.ProfessionalSummaryResponse;
import com.example.catalogo.professional.infrastructure.web.mapper.ProfessionalDtoMapper;
import com.example.catalogo.shared.infrastructure.web.catalog.RequiresCatalogReady;
import com.example.catalogo.shared.infrastructure.web.pagination.PaginationHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/professionals")
@RequiredArgsConstructor
public class ProfessionalController {

	private final ProfessionalService professionalService;
	private final ProfessionalDtoMapper professionalDtoMapper;

	@GetMapping
	@RequiresCatalogReady
	public ResponseEntity<List<ProfessionalSummaryResponse>> searchProfessionals(
			@Valid ProfessionalSearchRequest request) {
		PageResult<ProfessionalSummary> result = professionalService.searchProfessionals(
				professionalDtoMapper.toCriteria(request),
				professionalDtoMapper.toSortOrders(request),
				professionalDtoMapper.toPageQuery(request));
		List<ProfessionalSummaryResponse> professionals = result.getContent().stream()
				.map(professionalDtoMapper::toResponse)
				.toList();
		return ResponseEntity.ok()
				.headers(PaginationHeaders.of(result.getTotalElements(), result.getPage(), result.getSize()))
				.body(professionals);
	}

	// Spring parte por comas un parámetro que se guarda en una lista, y "sort=lastName,desc"
	// terminaría como dos valores sueltos. Con este editor cada parámetro sort queda entero.
	@InitBinder
	void keepEachSortParameterWhole(WebDataBinder binder) {
		binder.registerCustomEditor(List.class, "sort", new CustomCollectionEditor(List.class));
	}

}
