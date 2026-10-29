package com.example.catalogo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistrar;

/**
 * Hace en las pruebas el papel del servicio de turnos: genera en memoria un par de claves RSA, le
 * pasa al catálogo la ubicación de la clave pública (como en producción se la pasa un archivo de
 * {@code secrets/}) y permite firmar tokens con la privada. No hay claves en el repositorio.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestJwtKeysConfiguration {

	@Bean
	public KeyPair testJwtKeyPair() throws NoSuchAlgorithmException {
		return generateKeyPair();
	}

	@Bean
	public DynamicPropertyRegistrar jwtKeyProperties(KeyPair testJwtKeyPair) throws IOException {
		Path publicKeyFile = writePublicKeyFile(testJwtKeyPair.getPublic());
		return registry -> registry.add("catalogo.security.jwt.public-key-location", () -> publicKeyFile.toUri().toString());
	}

	public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		return generator.generateKeyPair();
	}

	public static Path writePublicKeyFile(PublicKey publicKey) throws IOException {
		String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(publicKey.getEncoded());
		Path file = Files.createTempFile("jwt-public-test", ".pem");
		file.toFile().deleteOnExit();
		Files.writeString(file, "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----\n");
		return file;
	}

	// Firma un token con el mismo formato que emite turnos: login en sub y roles en auth (ADR-0060).
	public static String signToken(KeyPair keyPair, String login, Instant issuedAt, Instant expiresAt, String... authorities) {
		RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
				.privateKey((RSAPrivateKey) keyPair.getPrivate())
				.build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(login)
				.claim("auth", List.of(authorities))
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.build();
		JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)))
				.encode(JwtEncoderParameters.from(header, claims))
				.getTokenValue();
	}

}
