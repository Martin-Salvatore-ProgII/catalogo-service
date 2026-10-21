package com.example.catalogo.sync.infrastructure.persistence.repository;

import java.time.Instant;

import com.example.catalogo.sync.infrastructure.persistence.entity.SyncStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

// Cada cambio es un UPDATE sobre la única fila. clearAutomatically hace que una lectura posterior
// en la misma transacción vea lo que quedó en la base y no una copia vieja en memoria.
@Repository
public interface JpaSyncStateRepository extends JpaRepository<SyncStateEntity, Integer> {

	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "UPDATE sync_state SET sync_in_progress = TRUE WHERE id = 1", nativeQuery = true)
	int markSyncStarted();

	// La condición sobre la versión va en el mismo UPDATE, así la verifica la base (ADR-0007).
	// IS NOT DISTINCT FROM también da verdadero con NULL = NULL: la primera sincronización, sin
	// versión local. Es SQL nativo porque JPQL no tiene ese operador.
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE sync_state
			SET local_version = :newVersion,
				sync_in_progress = FALSE,
				last_success_at = :succeededAt,
				last_success_type = :type
			WHERE id = 1 AND local_version IS NOT DISTINCT FROM CAST(:expectedVersion AS BIGINT)
			""", nativeQuery = true)
	int updateVersionIfCurrentIs(@Param("expectedVersion") Long expectedVersion, @Param("newVersion") long newVersion,
			@Param("type") String type, @Param("succeededAt") Instant succeededAt);

	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = """
			UPDATE sync_state
			SET sync_in_progress = FALSE, last_error = :error, last_error_at = :failedAt
			WHERE id = 1
			""", nativeQuery = true)
	int recordFailure(@Param("error") String error, @Param("failedAt") Instant failedAt);

	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query(value = "UPDATE sync_state SET sync_in_progress = FALSE WHERE id = 1", nativeQuery = true)
	int clearSyncInProgress();

}
