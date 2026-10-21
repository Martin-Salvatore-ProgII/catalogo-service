package com.example.catalogo.professional.infrastructure.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.example.catalogo.shared.infrastructure.web.pagination.MaxPageSize;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

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

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate date;

	// ISO acepta HH:mm y HH:mm:ss, como pide el contrato.
	@DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
	private LocalTime timeFrom;

	@DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
	private LocalTime timeTo;

	@Min(0)
	private Integer page;

	@Min(1)
	@MaxPageSize
	private Integer size;

	// Reglas de la franja (ADR-0065, HU-05 criterio 8). Son de formato de la entrada, por eso van acá y
	// no en el caso de uso. Cada una sale en fieldErrors con su propio nombre.

	@AssertTrue(message = "timeFrom and timeTo must be given together")
	public boolean isTimeRangeComplete() {
		return (timeFrom == null) == (timeTo == null);
	}

	@AssertTrue(message = "a time range requires a date")
	public boolean isTimeRangeWithDate() {
		return timeFrom == null || date != null;
	}

	@AssertTrue(message = "timeFrom must be before timeTo")
	public boolean isTimeRangeOrdered() {
		return timeFrom == null || timeTo == null || timeFrom.isBefore(timeTo);
	}

	private List<@Pattern(regexp = SORT_PATTERN, message = "must be firstName, lastName or id, optionally followed by asc or desc") String> sort;

}
