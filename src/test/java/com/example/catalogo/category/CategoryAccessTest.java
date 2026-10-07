package com.example.catalogo.category;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;

import com.example.catalogo.TestJwtKeysConfiguration;
import com.example.catalogo.TestcontainersConfiguration;
import com.example.catalogo.category.infrastructure.persistence.entity.CategoryEntity;
import com.example.catalogo.category.infrastructure.persistence.repository.JpaCategoryRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Seguridad del endpoint de categorías con la aplicación completa, PostgreSQL real y la cadena de
 * filtros real (RNF-04). Los tokens se firman con la clave privada de prueba, que hace el papel
 * del servicio de turnos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, TestJwtKeysConfiguration.class })
class CategoryAccessTest {

	private static final String CATEGORIES = "/api/professional-categories";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private KeyPair testJwtKeyPair;

	@Autowired
	private JpaCategoryRepository jpaCategoryRepository;

	@BeforeEach
	void storeCategories() {
		jpaCategoryRepository.deleteAll();
		jpaCategoryRepository.save(category(102L, "Pediatria", true));
		jpaCategoryRepository.save(category(101L, "Cardiologia", true));
		jpaCategoryRepository.save(category(108L, "Salud mental", false));
	}

	// Estas pruebas guardan datos de verdad, sin transacción que los deshaga: se limpian al
	// terminar para no afectar a otras pruebas que usen la misma base.
	@AfterEach
	void removeCategories() {
		jpaCategoryRepository.deleteAll();
	}

	// --- Los tres casos que tienen que rechazarse ---

	@Test
	void withoutTokenIsRejected() throws Exception {
		mockMvc.perform(get(CATEGORIES))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(""));
	}

	@Test
	void withExpiredTokenIsRejected() throws Exception {
		Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
		String expired = TestJwtKeysConfiguration.signToken(
				testJwtKeyPair, "juan", twoHoursAgo, twoHoursAgo.plus(Duration.ofHours(1)), "ROLE_USER");

		mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(""));
	}

	@Test
	void withTokenSignedByAnotherKeyIsRejected() throws Exception {
		// Un token bien formado y vigente, con rol de administrador, pero firmado con una clave
		// que no es la de turnos: cualquiera puede fabricarlo, y por eso no tiene que servir.
		KeyPair anotherKeyPair = TestJwtKeysConfiguration.generateKeyPair();
		Instant now = Instant.now();
		String forged = TestJwtKeysConfiguration.signToken(
				anotherKeyPair, "juan", now, now.plus(Duration.ofHours(1)), "ROLE_USER", "ROLE_ADMIN");

		mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
				.andExpect(status().isUnauthorized())
				.andExpect(content().string(""));
	}

	// --- El caso que tiene que funcionar ---

	@Test
	void withValidTokenReturnsTheEnabledCategoriesOrderedByName() throws Exception {
		Instant now = Instant.now();
		String token = TestJwtKeysConfiguration.signToken(
				testJwtKeyPair, "juan", now, now.plus(Duration.ofHours(1)), "ROLE_USER");

		mockMvc.perform(get(CATEGORIES).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Cardiologia"))
				.andExpect(jsonPath("$[1].name").value("Pediatria"));
	}

	private CategoryEntity category(Long id, String name, boolean enabled) {
		return CategoryEntity.builder()
				.id(id)
				.name(name)
				.description("Descripcion de " + name)
				.enabled(enabled)
				.createdAt(Instant.parse("2026-09-01T03:00:00Z"))
				.updatedAt(Instant.parse("2026-09-01T03:00:00Z"))
				.build();
	}

}
