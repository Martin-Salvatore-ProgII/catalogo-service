package com.example.catalogo.shared.infrastructure.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.interfaces.RSAPublicKey;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Validación del JWT de usuario. Este servicio no emite tokens: solo tiene la clave pública del
 * par, con la que comprueba que un token fue firmado por el servicio de turnos (ADR-0032).
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	// Nombre del claim con los roles. Es parte del contrato con turnos, que lo escribe al emitir.
	public static final String AUTHORITIES_CLAIM = "auth";

	// La clave pública llega como archivo externo, exportada de la clave privada de turnos
	// (ADR-0043, ADR-0060). No es un secreto, pero se configura igual que uno para no atar el
	// código a un par de claves.
	@Bean
	public RSAPublicKey jwtPublicKey(JwtProperties properties) throws IOException {
		try (InputStream pem = properties.publicKeyLocation().getInputStream()) {
			return RsaKeyConverters.x509().convert(pem);
		}
	}

	// Valida la firma con la clave pública y rechaza los tokens vencidos.
	@Bean
	public JwtDecoder jwtDecoder(RSAPublicKey jwtPublicKey) {
		return NimbusJwtDecoder.withPublicKey(jwtPublicKey).build();
	}

	// Los roles vienen en el claim "auth" tal cual se usan (ROLE_USER, ROLE_ADMIN). Por defecto
	// Spring los buscaría en otro claim y les agregaría un prefijo.
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
		authoritiesConverter.setAuthoritiesClaimName(AUTHORITIES_CLAIM);
		authoritiesConverter.setAuthorityPrefix("");
		JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
		authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
		return authenticationConverter;
	}

}
