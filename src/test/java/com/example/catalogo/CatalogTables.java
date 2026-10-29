package com.example.catalogo;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Deja vacías las tablas del catálogo, para las pruebas con la aplicación completa que necesitan
 * sus propios datos: al arrancar, el servicio ya cargó el snapshot de la cátedra falsa
 * ({@link TestCatedraConfiguration}). Se borra de hijos a padres, por las claves foráneas.
 */
public final class CatalogTables {

	private CatalogTables() {
	}

	public static void clear(JdbcClient jdbcClient) {
		jdbcClient.sql("DELETE FROM weekly_schedule").update();
		jdbcClient.sql("DELETE FROM professional").update();
		jdbcClient.sql("DELETE FROM professional_category").update();
	}

	// Vuelve el estado de sincronización a como lo deja la migración: sin versión local.
	public static void resetSyncState(JdbcClient jdbcClient) {
		jdbcClient.sql("""
				UPDATE sync_state SET local_version = NULL, sync_in_progress = FALSE, last_success_at = NULL,
					last_success_type = NULL, last_error_at = NULL, last_error = NULL
				""").update();
	}

}
