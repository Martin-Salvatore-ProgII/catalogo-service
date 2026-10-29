package com.example.catalogo.professional.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.category.domain.ports.out.CategoryRepository;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.ProfessionalAgenda;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.professional.domain.ports.out.ProfessionalRepository;
import com.example.catalogo.professional.domain.ports.out.WeeklyScheduleRepository;
import com.example.catalogo.sync.domain.model.SyncState;
import com.example.catalogo.sync.domain.ports.out.SyncStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetProfessionalAgendaUseCaseImplTest {

	@Mock
	private ProfessionalRepository professionalRepository;

	@Mock
	private WeeklyScheduleRepository weeklyScheduleRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private SyncStateRepository syncStateRepository;

	@InjectMocks
	private GetProfessionalAgendaUseCaseImpl useCase;

	@Test
	void returnsEmptyWhenTheProfessionalDoesNotExist() {
		when(professionalRepository.findById(999L)).thenReturn(Optional.empty());

		assertThat(useCase.getProfessionalAgenda(999L)).isEmpty();
		verifyNoInteractions(weeklyScheduleRepository, syncStateRepository);
	}

	@Test
	void returnsTheProfessionalWithItsCategorySchedulesAndCatalogVersion() {
		List<WeeklySchedule> schedules = List.of(schedule(301L));
		storedProfessional(true, true, schedules);

		ProfessionalAgenda agenda = useCase.getProfessionalAgenda(201L).orElseThrow();

		assertThat(agenda.getId()).isEqualTo(201L);
		assertThat(agenda.getFirstName()).isEqualTo("Ana");
		assertThat(agenda.getLastName()).isEqualTo("Gómez");
		assertThat(agenda.getCategory().getName()).isEqualTo("Cardiologia");
		assertThat(agenda.isEnabled()).isTrue();
		assertThat(agenda.getWeeklySchedules()).isEqualTo(schedules);
		assertThat(agenda.getCatalogVersion()).isEqualTo(7L);
	}

	@Test
	void aDisabledProfessionalIsReturnedMarkedAsDisabled() {
		storedProfessional(false, true, List.of());

		assertThat(useCase.getProfessionalAgenda(201L).orElseThrow().isEnabled()).isFalse();
	}

	@Test
	void anEnabledProfessionalOfADisabledCategoryIsNotEnabled() {
		storedProfessional(true, false, List.of(schedule(301L)));

		ProfessionalAgenda agenda = useCase.getProfessionalAgenda(201L).orElseThrow();

		// ADR-0014: se devuelve igual, con sus horarios, pero marcado como no habilitado.
		assertThat(agenda.isEnabled()).isFalse();
		assertThat(agenda.getWeeklySchedules()).hasSize(1);
	}

	private void storedProfessional(boolean enabled, boolean categoryEnabled, List<WeeklySchedule> schedules) {
		when(professionalRepository.findById(201L)).thenReturn(Optional.of(Professional.builder()
				.id(201L).categoryId(101L).firstName("Ana").lastName("Gómez").enabled(enabled).build()));
		when(categoryRepository.findById(101L)).thenReturn(Optional.of(
				Category.builder().id(101L).name("Cardiologia").enabled(categoryEnabled).build()));
		when(weeklyScheduleRepository.findEnabledByProfessionalOrderedByDayAndTime(201L)).thenReturn(schedules);
		when(syncStateRepository.getCurrent()).thenReturn(SyncState.builder().localVersion(7L).build());
	}

	private WeeklySchedule schedule(Long id) {
		return WeeklySchedule.builder().id(id).professionalId(201L).dayOfWeek(DayOfWeek.MONDAY)
				.startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(12, 0)).slotDurationMinutes(30).enabled(true).build();
	}

}
