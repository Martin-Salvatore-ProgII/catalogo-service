package com.example.catalogo.sync;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;

import com.example.catalogo.TestCatedraConfiguration;
import com.example.catalogo.TestJwtKeysConfiguration;
import com.example.catalogo.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El estado de sincronización y el 503 CATALOG_NOT_READY con la aplicación completa: la seguridad,
 * el interceptor de {@code @RequiresCatalogReady} y el manejador de errores reales. Al arrancar, el
 * servicio ya cargó el snapshot de la cátedra falsa, así que el catálogo empieza listo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, TestJwtKeysConfiguration.class, TestCatedraConfiguration.class })
class CatalogReadinessAccessTest {

	private static final String CATEGORIES = "/api/professional-categories";
	private static final String SYNC_STATUS = "/api/catalog/sync-status";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private KeyPair testJwtKeyPair;

	@Autowired
	private JdbcClient jdbcClient;

	// Las pruebas que borran la versión la devuelven al terminar: el contexto lo comparten otras.
	@AfterEach
	void restoreTheLocalVersion() {
		jdbcClient.sql("UPDATE sync_state SET local_version = 1").update();
	}

	@Test
	void syncStatusShowsTheSnapshotLoadedAtStartup() throws Exception {
		mockMvc.perform(get(SYNC_STATUS).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.localVersion").value(1))
				.andExpect(jsonPath("$.lastSuccessType").value("SNAPSHOT"))
				.andExpect(jsonPath("$.syncInProgress").value(false));
	}

	@Test
	void syncStatusRequiresAToken() throws Exception {
		mockMvc.perform(get(SYNC_STATUS))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(""));
	}

	@Test
	void categoriesAnswerWhenTheCatalogIsReady() throws Exception {
		mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk());
	}

	@Test
	void categoriesAnswerCatalogNotReadyWithoutALocalVersion() throws Exception {
		jdbcClient.sql("UPDATE sync_state SET local_version = NULL").update();

		mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isServiceUnavailable())
				.andExpect(content().contentTypeCompatibleWith("application/problem+json"))
				.andExpect(jsonPath("$.code").value("CATALOG_NOT_READY"))
				.andExpect(jsonPath("$.path").value(CATEGORIES));
	}

	@Test
	void syncStatusStillAnswersWithoutALocalVersion() throws Exception {
		jdbcClient.sql("UPDATE sync_state SET local_version = NULL").update();

		mockMvc.perform(get(SYNC_STATUS).header(HttpHeaders.AUTHORIZATION, bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.localVersion").isEmpty());
	}

	@Test
	void authenticationIsCheckedBeforeTheCatalogReadiness() throws Exception {
		// Sin token, 401 aunque el catálogo no esté listo: no se revela nada a quien no se autenticó.
		jdbcClient.sql("UPDATE sync_state SET local_version = NULL").update();

		mockMvc.perform(get(CATEGORIES)).andExpect(status().isUnauthorized());
	}

	private String bearer() {
		Instant now = Instant.now();
		return "Bearer " + TestJwtKeysConfiguration.signToken(testJwtKeyPair, "juan", now, now.plus(Duration.ofHours(1)), "ROLE_USER");
	}

}
