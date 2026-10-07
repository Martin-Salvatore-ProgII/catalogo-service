package com.example.catalogo.shared.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;

import com.example.catalogo.TestJwtKeysConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;

// Sin Spring: se arma la configuración a mano con una clave pública leída de un archivo PEM,
// como en producción, y se validan tokens firmados con la privada de ese par (el papel de turnos).
class JwtConfigTest {

	private final JwtConfig jwtConfig = new JwtConfig();

	private KeyPair keyPair;
	private JwtDecoder jwtDecoder;

	@BeforeEach
	void setUp() throws Exception {
		keyPair = TestJwtKeysConfiguration.generateKeyPair();
		JwtProperties properties = new JwtProperties(
				new FileSystemResource(TestJwtKeysConfiguration.writePublicKeyFile(keyPair.getPublic())));
		jwtDecoder = jwtConfig.jwtDecoder(jwtConfig.jwtPublicKey(properties));
	}

	@Test
	void acceptsATokenSignedWithTheMatchingPrivateKey() {
		Jwt jwt = jwtDecoder.decode(validToken(keyPair, "ROLE_USER"));

		assertThat(jwt.getSubject()).isEqualTo("juan");
	}

	@Test
	void rejectsATokenSignedWithAnotherKey() throws Exception {
		KeyPair anotherKeyPair = TestJwtKeysConfiguration.generateKeyPair();

		assertThatThrownBy(() -> jwtDecoder.decode(validToken(anotherKeyPair, "ROLE_ADMIN")))
				.isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsAnExpiredToken() {
		Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
		String expired = TestJwtKeysConfiguration.signToken(
				keyPair, "juan", twoHoursAgo, twoHoursAgo.plus(Duration.ofHours(1)), "ROLE_USER");

		assertThatThrownBy(() -> jwtDecoder.decode(expired)).isInstanceOf(JwtValidationException.class);
	}

	@Test
	void rejectsATextThatIsNotAToken() {
		assertThatThrownBy(() -> jwtDecoder.decode("no-es-un-jwt")).isInstanceOf(JwtException.class);
	}

	@Test
	void readsTheLoginAndTheRolesFromTheToken() {
		Jwt jwt = jwtDecoder.decode(validToken(keyPair, "ROLE_USER", "ROLE_ADMIN"));

		AbstractAuthenticationToken authentication = jwtConfig.jwtAuthenticationConverter().convert(jwt);

		assertThat(authentication.getName()).isEqualTo("juan");
		// Spring Security agrega por su cuenta una marca de cómo se autenticó el pedido
		// (FACTOR_BEARER); por eso se verifica que estén los roles, no que sean los únicos.
		assertThat(authentication.getAuthorities())
				.extracting(GrantedAuthority::getAuthority)
				.contains("ROLE_USER", "ROLE_ADMIN")
				.doesNotContain("SCOPE_ROLE_USER", "ROLE_ROLE_USER");
	}

	private String validToken(KeyPair keys, String... authorities) {
		Instant now = Instant.now();
		return TestJwtKeysConfiguration.signToken(keys, "juan", now, now.plus(Duration.ofHours(1)), authorities);
	}

}
