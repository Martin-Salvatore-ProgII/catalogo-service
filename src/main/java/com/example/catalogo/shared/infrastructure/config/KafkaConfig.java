package com.example.catalogo.shared.infrastructure.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

/**
 * Consumidor de Kafka de la cátedra. Los servidores, el consumer group y el topic salen de la
 * integración que se pide al arrancar (ADR-0005), no de las propiedades {@code spring.kafka.*}.
 *
 * <p>Este servicio solo consume {@code CatalogUpdated}. Los valores llegan como texto y el
 * adaptador de entrada los interpreta: un aviso mal formado igual dispara el ciclo (CU-01, 1b), así
 * que el deserializador no puede ser el que falle.
 */
@Configuration
public class KafkaConfig {

	@Bean
	public ConsumerFactory<String, String> catedraConsumerFactory(CatedraIntegration integration) {
		Map<String, Object> config = new HashMap<>();
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, integration.kafkaBootstrapServers());
		config.put(ConsumerConfig.GROUP_ID_CONFIG, integration.kafkaConsumerGroupId());
		config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		// El offset se confirma a mano, recién después de que el ciclo terminó (REF §16).
		config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		// Sin offset guardado se empieza por lo nuevo: el ciclo del arranque ya pone la copia al
		// día, y releer los avisos viejos solo llenaría PROCESSED_EVENT de duplicados.
		config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		return new DefaultKafkaConsumerFactory<>(config);
	}

	// Se llama como la que Spring Boot crearía por defecto, para reemplazarla.
	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
			ConsumerFactory<String, String> catedraConsumerFactory) {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(catedraConsumerFactory);
		// El listener recibe un Acknowledgment y lo confirma cuando decide; el commit sale en el
		// momento, no agrupado.
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
		return factory;
	}

}
