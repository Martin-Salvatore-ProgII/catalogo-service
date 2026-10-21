package com.example.catalogo.professional.infrastructure.web.dto;

import java.util.List;

import com.example.catalogo.shared.infrastructure.web.pagination.MaxPageSize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Los parámetros de GET /api/professionals (docs/arq/contratos.md). Todos opcionales, y con tipos
// envoltorio: un parámetro que no viene queda en null.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalSearchRequest {

	// Un campo de orden de la lista cerrada, otros separados por coma, y la dirección opcional al
	// final: "lastName,firstName", "lastName,desc". Es la convención de Spring Data que usa la REF.
	private static final String SORT_FIELD = "(firstName|lastName|id)";
	private static final String SORT_PATTERN = "^" + SORT_FIELD + "(," + SORT_FIELD + ")*(,(?i:asc|desc))?$";

	private Long categoryId;

	@Size(min = 1, max = 100)
	@Pattern(regexp = ".*\\S.*", message = "must not be blank")
	private String name;

	// Sin el parámetro queda en null, y el caso de uso aplica el valor por defecto (true).
	private Boolean enabled;

	@Min(0)
	private Integer page;

	@Min(1)
	@MaxPageSize
	private Integer size;

	private List<@Pattern(regexp = SORT_PATTERN, message = "must be firstName, lastName or id, optionally followed by asc or desc") String> sort;

}
