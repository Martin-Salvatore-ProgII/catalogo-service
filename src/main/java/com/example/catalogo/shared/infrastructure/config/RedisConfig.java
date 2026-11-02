package com.example.catalogo.shared.infrastructure.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Conexión al Redis de la cátedra. Host, puerto y credenciales no están en el {@code .env}: salen
 * de la integración que se pide al arrancar (ADR-0005), por eso la fábrica se arma por código y no
 * con las propiedades {@code spring.data.redis.*}.
 *
 * <p>Solo se lee. La ACL de la cátedra permite leer {@code catedra:sync:*} y escribir únicamente
 * en {@code alumnos:{groupId}:*}; este servicio no usa ese namespace (ADR-0011, REF §14.6).
 */
@Configuration
public class RedisConfig {

	@Bean
	public RedisConnectionFactory catedraRedisConnectionFactory(
			CatedraIntegration integration, CatedraProperties properties) {
		RedisStandaloneConfiguration server = new RedisStandaloneConfiguration(
				integration.redisHost(), integration.redisPort());
		server.setUsername(integration.redisUsername());
		server.setPassword(integration.redisPassword());

		// Mismos timeouts que hacia la cátedra REST (ADR-0029, RNF-09). Sin ellos, un Redis que
		// acepta la conexión y no responde dejaría el ciclo de sincronización esperando.
		// REJECT_COMMANDS: con la conexión caída, el comando falla en el momento. Si se encolara,
		// se ejecutaría solo al reconectar, un reintento que no decidimos y que no quedaría en el
		// log (robustez.md, principio 7). Cuando el ciclo falla, lo retoma el próximo disparador.
		LettuceClientConfiguration client = LettuceClientConfiguration.builder()
				.commandTimeout(properties.readTimeout())
				.clientOptions(ClientOptions.builder()
						.socketOptions(SocketOptions.builder().connectTimeout(properties.connectTimeout()).build())
						.disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
						.build())
				.build();
		// La conexión se abre en el primer comando, no al arrancar: un Redis caído no impide que el
		// servicio arranque y atienda búsquedas con la copia local (ENUNCIADO §8).
		return new LettuceConnectionFactory(server, client);
	}

	@Bean
	public StringRedisTemplate catedraRedisTemplate(RedisConnectionFactory catedraRedisConnectionFactory) {
		return new StringRedisTemplate(catedraRedisConnectionFactory);
	}

}
