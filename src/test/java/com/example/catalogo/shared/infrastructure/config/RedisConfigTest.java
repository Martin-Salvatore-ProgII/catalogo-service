package com.example.catalogo.shared.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

// Sin Spring: se arma la configuración a mano con una integración inventada. No se abre ninguna
// conexión (la fábrica conecta recién con el primer comando).
class RedisConfigTest {

	private static final CatedraIntegration INTEGRATION = new CatedraIntegration(
			"grupo-de-prueba", "redis.prueba.internal", 6380, "usuario-de-prueba", "clave-de-prueba",
			"kafka.prueba.internal:9092", "grupo-consumidor", "topic-de-prueba", "PROVISIONED");

	private static final CatedraProperties PROPERTIES = new CatedraProperties(
			"http://api.prueba.internal", "jwt-de-prueba", Duration.ofSeconds(2), Duration.ofSeconds(5));

	private final LettuceConnectionFactory factory =
			(LettuceConnectionFactory) new RedisConfig().catedraRedisConnectionFactory(INTEGRATION, PROPERTIES);

	@Test
	void takesHostPortAndCredentialsFromTheIntegration() {
		assertThat(factory.getHostName()).isEqualTo("redis.prueba.internal");
		assertThat(factory.getPort()).isEqualTo(6380);
		assertThat(factory.getStandaloneConfiguration().getUsername()).isEqualTo("usuario-de-prueba");
		assertThat(factory.getStandaloneConfiguration().getPassword().get()).isEqualTo("clave-de-prueba".toCharArray());
	}

	@Test
	void usesTheSameTimeoutsAsTheCatedraRestClient() {
		assertThat(factory.getClientConfiguration().getCommandTimeout()).isEqualTo(Duration.ofSeconds(5));
		assertThat(factory.getClientConfiguration().getClientOptions().orElseThrow().getSocketOptions().getConnectTimeout())
				.isEqualTo(Duration.ofSeconds(2));
	}

	@Test
	void rejectsCommandsWhileDisconnectedInsteadOfQueueingThem() {
		assertThat(factory.getClientConfiguration().getClientOptions().orElseThrow().getDisconnectedBehavior())
				.isEqualTo(io.lettuce.core.ClientOptions.DisconnectedBehavior.REJECT_COMMANDS);
	}

}
