package com.example.catalogo.shared.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

// Sin Spring ni broker: solo se comprueba qué configuración recibe el consumidor.
class KafkaConfigTest {

	private static final CatedraIntegration INTEGRATION = new CatedraIntegration(
			"grupo-de-prueba", "redis.prueba.internal", 6379, "usuario", "clave",
			"kafka.prueba.internal:9092", "grupo-consumidor", "topic-de-prueba", "PROVISIONED");

	private final KafkaConfig config = new KafkaConfig();
	private final ConsumerFactory<String, String> consumerFactory = config.catedraConsumerFactory(INTEGRATION);

	@Test
	void takesServersAndConsumerGroupFromTheIntegration() {
		Map<String, Object> properties = consumerFactory.getConfigurationProperties();

		assertThat(properties).containsEntry("bootstrap.servers", "kafka.prueba.internal:9092");
		assertThat(properties).containsEntry("group.id", "grupo-consumidor");
	}

	@Test
	void readsRawTextSoAMalformedNoticeStillReachesTheListener() {
		Map<String, Object> properties = consumerFactory.getConfigurationProperties();

		assertThat(properties).containsEntry("key.deserializer", StringDeserializer.class);
		assertThat(properties).containsEntry("value.deserializer", StringDeserializer.class);
	}

	@Test
	void doesNotCommitOffsetsOnItsOwnAndStartsFromTheLatest() {
		Map<String, Object> properties = consumerFactory.getConfigurationProperties();

		assertThat(properties).containsEntry("enable.auto.commit", false);
		assertThat(properties).containsEntry("auto.offset.reset", "latest");
	}

	@Test
	void listenerAcknowledgesManuallyAndImmediately() {
		ConcurrentKafkaListenerContainerFactory<String, String> factory =
				config.kafkaListenerContainerFactory(consumerFactory);

		assertThat(factory.getContainerProperties().getAckMode()).isEqualTo(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
	}

}
