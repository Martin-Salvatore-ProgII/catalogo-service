package com.example.catalogo.sync.infrastructure.redis.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import com.example.catalogo.category.domain.model.Category;
import com.example.catalogo.professional.domain.model.Professional;
import com.example.catalogo.professional.domain.model.WeeklySchedule;
import com.example.catalogo.sync.application.exception.CatalogChangesException;
import com.example.catalogo.sync.domain.model.CatalogVersions;
import com.example.catalogo.sync.domain.model.VersionChanges;
import com.example.catalogo.sync.domain.ports.out.CatalogChangesPort;
import com.example.catalogo.sync.domain.ports.out.CatalogVersionPort;
import com.example.catalogo.sync.infrastructure.catedra.dto.CatedraCategoryDto;
import com.example.catalogo.sync.infrastructure.catedra.dto.CatedraProfessionalDto;
import com.example.catalogo.sync.infrastructure.catedra.dto.CatedraWeeklyScheduleDto;
import com.example.catalogo.sync.infrastructure.catedra.mapper.CatedraEntityMapper;
import com.example.catalogo.sync.infrastructure.redis.dto.VersionChangesResponse;
import com.example.catalogo.sync.infrastructure.redis.mapper.VersionChangesMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lee del Redis de la cátedra las versiones, los cambios de cada versión y el estado actual de las
 * entidades (REF 14). Solo lee: la catedra no permite escribir en {@code catedra:sync:*}.
 *
 * <p>Una falla de Redis es una {@link CatalogChangesException}. Un valor ausente o ilegible no lo
 * es: se devuelve como "no está", porque en los dos casos el estado no se puede verificar y la
 * decisión es del caso de uso.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCatalogAdapter implements CatalogVersionPort, CatalogChangesPort {

	static final String CURRENT_VERSION_KEY = "catedra:sync:current-version";
	static final String OLDEST_VERSION_KEY = "catedra:sync:oldest-available-version";
	static final String CHANGES_KEY_PREFIX = "catedra:sync:changes:";  //los IDs que cambiaron en la version
	//el estado actual de las entidades
	static final String CATEGORIES_KEY = "catedra:sync:professional-categories";
	static final String PROFESSIONALS_KEY = "catedra:sync:professionals";
	static final String WEEKLY_SCHEDULES_KEY = "catedra:sync:weekly-schedules";

	private final StringRedisTemplate redisTemplate;
	private final JsonMapper jsonMapper;
	private final VersionChangesMapper versionChangesMapper;
	private final CatedraEntityMapper entityMapper;

	@Override
	public CatalogVersions getVersions() {
		// Dos lecturas de un String cada una. Sin las dos no hay con qué comparar
		long current = readVersion(CURRENT_VERSION_KEY);
		long oldest = readVersion(OLDEST_VERSION_KEY);
		return CatalogVersions.builder().current(current).oldestAvailable(oldest).build();
	}

	// parsea y valida el changes de la catedra 
	@Override
	public Optional<VersionChanges> findChanges(long version) {
		String json = call(() -> redisTemplate.opsForValue().get(CHANGES_KEY_PREFIX + version));
		if (json == null) {
			return Optional.empty();
		}
		VersionChangesResponse response = parse(json, VersionChangesResponse.class, CHANGES_KEY_PREFIX + version);
		// Los tres arreglos son obligatorios (REF 14.5), y la clave tiene que ser de la versión que se pidió
		boolean valid = response != null && response.getVersion() != null && response.getVersion() == version
				&& response.getChanges() != null && response.getChanges().getProfessionalCategories() != null
				&& response.getChanges().getProfessionals() != null && response.getChanges().getWeeklySchedules() != null;
		if (!valid) {
			log.warn("Unreadable or inconsistent changes for catalog version {}", version);
			return Optional.empty(); // si algo era inconsistente, empty
		}
		return Optional.of(versionChangesMapper.toDomainModel(response));
	}

	@Override
	public List<Category> findCategories(Collection<Long> ids) {
		return findEntities(CATEGORIES_KEY, ids, CatedraCategoryDto.class, CatedraCategoryDto::getId, entityMapper::toCategory);
	}

	@Override
	public List<Professional> findProfessionals(Collection<Long> ids) {
		return findEntities(PROFESSIONALS_KEY, ids, CatedraProfessionalDto.class, CatedraProfessionalDto::getId,
				entityMapper::toProfessional);
	}

	@Override
	public List<WeeklySchedule> findWeeklySchedules(Collection<Long> ids) {
		return findEntities(WEEKLY_SCHEDULES_KEY, ids, CatedraWeeklyScheduleDto.class, CatedraWeeklyScheduleDto::getId,
				entityMapper::toWeeklySchedule);
	}

	// Un solo HashMultipleGET, permite pedir datos especificos de un Hash de Redis
	private <D, M> List<M> findEntities(
			String key, Collection<Long> ids, Class<D> type, Function<D, Long> idOf, Function<D, M> toDomain) {
		if (ids.isEmpty()) {
			return List.of();
		}
		List<Object> fields = ids.stream().map(id -> (Object) String.valueOf(id)).toList();
		List<Object> values = call(() -> redisTemplate.opsForHash().multiGet(key, fields));
		return IntStream.range(0, fields.size())
				.mapToObj(i -> {
					Object value = values.get(i);
					if (value == null) {
						return null;
					}
					D dto = parse(value.toString(), type, key + "[" + fields.get(i) + "]");
					// El field es el id (REF 14.4): un valor con otro id no es el que se pidió.
					if (dto == null || !Objects.equals(idOf.apply(dto), Long.valueOf(fields.get(i).toString()))) {
						log.warn("Unreadable or inconsistent entity {}[{}]", key, fields.get(i));
						return null;
					}
					return toDomain.apply(dto);
				})
				.filter(Objects::nonNull)
				.toList();
	}

	private long readVersion(String key) {
		String value = call(() -> redisTemplate.opsForValue().get(key));
		if (value == null) {
			throw new CatalogChangesException("Unexpected response from the catedra Redis: missing " + key);
		}
		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			throw new CatalogChangesException("Unexpected response from the catedra Redis: unreadable " + key);
		}
	}

	// Un JSON ilegible devuelve null: nunca se loguea su contenido, solo de qué clave era.
	private <T> T parse(String json, Class<T> type, String origin) {
		try {
			return jsonMapper.readValue(json, type);
		} catch (JacksonException e) {
			log.warn("Unreadable JSON at {}", origin);
			return null;
		}
	}

	// El mensaje original de Redis incluye el host: solo se informa el tipo de falla (RNF-02). No hay
	// reintento: si el ciclo falla, lo retoma el próximo disparador (ADR-0011).
	private <T> T call(Supplier<T> operation) {
		try {
			return operation.get();
		} catch (DataAccessException e) {
			throw new CatalogChangesException(
					"Catedra Redis unavailable (" + e.getMostSpecificCause().getClass().getSimpleName() + ")");
		}
	}

}
