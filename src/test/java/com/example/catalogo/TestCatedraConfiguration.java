package com.example.catalogo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;

/**
 * Le da al servicio una cátedra falsa que responde como la real: la integración en estado
 * PROVISIONED y el snapshot real. Al arrancar, el servicio pide la integración (sin ella no
 * arranca) y sincroniza desde el snapshot, así que toda prueba que levanta la aplicación completa
 * empieza con el catálogo real cargado. Ninguna prueba habla con la cátedra real (ADR-0044).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestCatedraConfiguration {

	// El Redis de la cátedra, con el mismo catálogo que publica el snapshot, en la versión 1 (como la
	// cátedra real). Se levanta antes que la cátedra falsa, que anuncia su dirección en la integración.
	@Bean
	public GenericContainer<?> redisContainer() {
		GenericContainer<?> redis = FakeRedis.newContainer();
		redis.start();
		FakeRedis.publishSnapshotAsVersion(FakeRedis.templateFor(redis), 1);
		return redis;
	}

	@Bean
	public GenericContainer<?> catedraContainer(GenericContainer<?> redisContainer) {
		return FakeCatedra.newContainerLikeTheCatedra(
				redisContainer.getHost(), redisContainer.getMappedPort(6379), FakeKafka.bootstrapServers());
	}

	// Los valores se leen recién cuando Spring los pide, con el contenedor ya levantado.
	@Bean
	public DynamicPropertyRegistrar catedraProperties(GenericContainer<?> catedraContainer) {
		return registry -> {
			registry.add("catalogo.catedra.api-url", () -> FakeCatedra.baseUrl(catedraContainer));
			registry.add("catalogo.catedra.technical-token", () -> FakeCatedra.TECHNICAL_TOKEN);
		};
	}

}
