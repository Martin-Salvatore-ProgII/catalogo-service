package com.example.catalogo.shared.infrastructure.config;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;

import com.example.catalogo.TestJwtKeysConfiguration;
import com.example.catalogo.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Prueba las reglas de acceso con la aplicación completa y la cadena de filtros real (RNF-04).
 * Los tokens se firman con la clave privada de prueba, que hace el papel del servicio de turnos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, TestJwtKeysConfiguration.class, SecurityConfigTest.TestController.class })
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private KeyPair testJwtKeyPair;

	@Test
	void healthIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	@Test
	void otherActuatorEndpointsAreNotPublic() throws Exception {
		mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
	}

	@Test
	void protectedRouteWithoutTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/test/whoami"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")));
	}

	@Test
	void unknownRouteWithoutTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/does-not-exist")).andExpect(status().isUnauthorized());
	}

	@Test
	void protectedRouteWithValidTokenIdentifiesTheUserByLogin() throws Exception {
		mockMvc.perform(get("/api/test/whoami").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken()))
				.andExpect(status().isOk())
				.andExpect(content().string("juan"));
	}

	@Test
	void unknownRouteWithValidTokenReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/does-not-exist").header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken()))
				.andExpect(status().isNotFound());
	}

	@Test
	void requestFromAWebOriginIsRejected() throws Exception {
		mockMvc.perform(get("/actuator/health").header(HttpHeaders.ORIGIN, "https://otro-sitio.example"))
				.andExpect(status().isForbidden());
	}

	@Test
	void preflightFromAWebOriginIsRejected() throws Exception {
		mockMvc.perform(options("/api/test/whoami")
				.header(HttpHeaders.ORIGIN, "https://otro-sitio.example")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden());
	}

	private String validToken() {
		Instant now = Instant.now();
		return TestJwtKeysConfiguration.signToken(testJwtKeyPair, "juan", now, now.plus(Duration.ofHours(1)), "ROLE_USER");
	}

	// Ruta solo para estas pruebas: el servicio todavía no tiene endpoints protegidos propios.
	@RestController
	static class TestController {

		@GetMapping("/api/test/whoami")
		String whoAmI(Authentication authentication) {
			return authentication.getName();
		}

	}

}
