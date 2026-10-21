package com.example.catalogo.professional.infrastructure.web.mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.professional.domain.model.PageQuery;
import com.example.catalogo.professional.domain.model.ProfessionalAgenda;
import com.example.catalogo.professional.domain.model.ProfessionalSearchCriteria;
import com.example.catalogo.professional.domain.model.ProfessionalSortField;
import com.example.catalogo.professional.domain.model.ProfessionalSortOrder;
import com.example.catalogo.professional.domain.model.ProfessionalSummary;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.professional.infrastructure.web.dto.CategoryRefResponse;
import com.example.catalogo.professional.infrastructure.web.dto.ProfessionalAgendaResponse;
import com.example.catalogo.professional.infrastructure.web.dto.ProfessionalSearchRequest;
import com.example.catalogo.professional.infrastructure.web.dto.ProfessionalSummaryResponse;
import com.example.catalogo.professional.infrastructure.web.dto.WeeklyScheduleResponse;
import com.example.catalogo.shared.infrastructure.config.PaginationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfessionalDtoMapper {

	private final PaginationProperties paginationProperties;

	public ProfessionalSearchCriteria toCriteria(ProfessionalSearchRequest request) {
		if (request == null) {
			return null;
		}
		return ProfessionalSearchCriteria.builder()
				.categoryId(request.getCategoryId())
				.name(request.getName())
				.enabled(request.getEnabled())
				.date(request.getDate())
				.timeFrom(request.getTimeFrom())
				.timeTo(request.getTimeTo())
				.build();
	}

	// Sin sort, la lista queda vacía y el caso de uso aplica el orden por defecto.
	public List<ProfessionalSortOrder> toSortOrders(ProfessionalSearchRequest request) {
		if (request == null || request.getSort() == null) {
			return List.of();
		}
		List<ProfessionalSortOrder> orders = new ArrayList<>();
		for (String sort : request.getSort()) {
			List<String> parts = Arrays.asList(sort.split(","));
			String last = parts.getLast();
			boolean hasDirection = last.equalsIgnoreCase("asc") || last.equalsIgnoreCase("desc");
			boolean descending = last.equalsIgnoreCase("desc");
			List<String> fields = hasDirection ? parts.subList(0, parts.size() - 1) : parts;
			for (String field : fields) {
				orders.add(ProfessionalSortOrder.builder().field(toSortField(field)).descending(descending).build());
			}
		}
		return orders;
	}

	public PageQuery toPageQuery(ProfessionalSearchRequest request) {
		if (request == null) {
			return null;
		}
		return PageQuery.builder()
				.page(request.getPage() != null ? request.getPage() : 0)
				.size(request.getSize() != null ? request.getSize() : paginationProperties.defaultSize())
				.build();
	}

	public ProfessionalSummaryResponse toResponse(ProfessionalSummary professional) {
		if (professional == null) {
			return null;
		}
		return ProfessionalSummaryResponse.builder()
				.id(professional.getId())
				.firstName(professional.getFirstName())
				.lastName(professional.getLastName())
				.category(toCategoryRef(professional.getCategory()))
				.enabled(professional.isEnabled())
				.build();
	}

	public ProfessionalAgendaResponse toResponse(ProfessionalAgenda agenda) {
		if (agenda == null) {
			return null;
		}
		return ProfessionalAgendaResponse.builder()
				.id(agenda.getId())
				.firstName(agenda.getFirstName())
				.lastName(agenda.getLastName())
				.category(toCategoryRef(agenda.getCategory()))
				.enabled(agenda.isEnabled())
				.weeklySchedules(agenda.getWeeklySchedules().stream().map(this::toScheduleResponse).toList())
				.catalogVersion(agenda.getCatalogVersion())
				.build();
	}

	private WeeklyScheduleResponse toScheduleResponse(WeeklySchedule schedule) {
		if (schedule == null) {
			return null;
		}
		return WeeklyScheduleResponse.builder()
				.id(schedule.getId())
				.dayOfWeek(schedule.getDayOfWeek())
				.startTime(schedule.getStartTime())
				.endTime(schedule.getEndTime())
				.slotDurationMinutes(schedule.getSlotDurationMinutes())
				.build();
	}

	private CategoryRefResponse toCategoryRef(Category category) {
		if (category == null) {
			return null;
		}
		return CategoryRefResponse.builder()
				.id(category.getId())
				.name(category.getName())
				.build();
	}

	// El request ya validó que el campo es uno de la lista cerrada.
	private ProfessionalSortField toSortField(String field) {
		return switch (field) {
			case "firstName" -> ProfessionalSortField.FIRST_NAME;
			case "lastName" -> ProfessionalSortField.LAST_NAME;
			case "id" -> ProfessionalSortField.ID;
			default -> throw new IllegalArgumentException("Unknown sort field: " + field);
		};
	}

}
