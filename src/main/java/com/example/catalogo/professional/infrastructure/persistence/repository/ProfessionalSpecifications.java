package com.example.catalogo.professional.infrastructure.persistence.repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import com.example.catalogo.professional.infrastructure.persistence.entity.ProfessionalEntity;
import com.example.catalogo.professional.infrastructure.persistence.entity.WeeklyScheduleEntity;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

/**
 * Los filtros de la búsqueda de profesionales, uno por método. Cada uno es una condición de la
 * consulta, y el adaptador combina con {@code and} solo los que se pidieron.
 */
public final class ProfessionalSpecifications {

	// Carácter de escape del LIKE, para que un % o un _ del texto buscado se tome literal.
	private static final char LIKE_ESCAPE = '\\';

	private ProfessionalSpecifications() {
	}

	public static Specification<ProfessionalEntity> hasCategory(Long categoryId) {
		return (root, query, cb) -> cb.equal(root.get("categoryId"), categoryId);
	}

	// Habilitado efectivo (ADR-0014), la misma regla que Professional.isEffectivelyEnabled. Sin
	// categorías habilitadas no hay ningún profesional habilitado: un IN vacío no es SQL válido.
	public static Specification<ProfessionalEntity> effectivelyEnabled(Set<Long> enabledCategoryIds) {
		return (root, query, cb) -> enabledCategoryIds.isEmpty()
				? cb.disjunction()
				: cb.and(cb.isTrue(root.get("enabled")), root.get("categoryId").in(enabledCategoryIds));
	}

	// Disponibilidad de agenda (ADR-0013): existe un horario habilitado de ese día que comparte al
	// menos un minuto con la franja (empieza antes de que ella termine y termina después de que
	// empiece). Los horarios son de la misma feature, así que se consultan acá (ADR-0064).
	public static Specification<ProfessionalEntity> hasScheduleOn(DayOfWeek day, LocalTime from, LocalTime to) {
		return (root, query, cb) -> {
			Subquery<Long> schedules = query.subquery(Long.class);
			Root<WeeklyScheduleEntity> schedule = schedules.from(WeeklyScheduleEntity.class);
			Predicate matches = cb.and(
					cb.equal(schedule.get("professionalId"), root.get("id")),
					cb.isTrue(schedule.get("enabled")),
					cb.equal(schedule.get("dayOfWeek"), day));
			if (from != null && to != null) {
				matches = cb.and(matches,
						cb.lessThan(schedule.<LocalTime>get("startTime"), to),
						cb.greaterThan(schedule.<LocalTime>get("endTime"), from));
			}
			return cb.exists(schedules.select(schedule.get("id")).where(matches));
		};
	}

	// Coincidencia parcial sobre "nombre apellido", sin mayúsculas ni tildes, como pide el puerto.
	// unaccent es la extensión de PostgreSQL de la migración V3: en otro motor cambiaría solo este método.
	public static Specification<ProfessionalEntity> nameContains(String text) {
		return (root, query, cb) -> {
			Expression<String> fullName = cb.concat(cb.concat(root.get("firstName"), " "), root.get("lastName"));
			String pattern = "%" + escapeLike(text) + "%";
			return cb.like(normalize(cb, fullName), normalize(cb, cb.literal(pattern)), LIKE_ESCAPE);
		};
	}

	private static Expression<String> normalize(CriteriaBuilder cb, Expression<String> text) {
		return cb.function("unaccent", String.class, cb.lower(text));
	}

	private static String escapeLike(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
