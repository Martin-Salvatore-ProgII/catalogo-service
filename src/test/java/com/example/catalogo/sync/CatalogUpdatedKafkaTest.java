package com.example.catalogo.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.UUID;

import com.example.catalogo.FakeKafka;
import com.example.catalogo.TestCatedraConfiguration;
import com.example.catalogo.TestJwtKeysConfiguration;
import com.example.catalogo.TestcontainersConfiguration;
import com.example.catalogo.sync.application.service.SyncService;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// El consumidor real, con un Kafka real: el topic y los servidores salen de la integración (ADR-0005),
// el aviso llega a la fachada y el offset se confirma después. La fachada es un mock: lo que se
// prueba acá es la conexión con Kafka, no el ciclo (CatalogIncrementalSyncTest).
@SpringBootTest(properties = "catalogo.sync.check-interval=1h")
@Import({ TestcontainersConfiguration.class, TestJwtKeysConfiguration.class, TestCatedraConfiguration.class })
class CatalogUpdatedKafkaTest {

	private static final String TOPIC = "catedra.catalog.proyecto-de-prueba";
	private static final String GROUP = "alumnos-proyecto-de-prueba";

	@MockitoBean
	private SyncService syncService;

	@Autowired
	private KafkaListenerEndpointRegistry registry;

	// Con `latest`, un aviso publicado antes de que el consumidor tenga su partición se perdería.
	@BeforeEach
	void waitForTheConsumerToBeAssigned() {
		await().atMost(Duration.ofSeconds(30)).until(() -> registry.getListenerContainers().stream()
				.anyMatch(container -> !container.getAssignedPartitions().isEmpty()));
	}

	@Test
	void deliversTheNoticeToTheServiceAndThenConfirmsTheOffset() {
		String eventId = "catalog-event-" + UUID.randomUUID();

		RecordMetadata sent = FakeKafka.publish(TOPIC, "7", notice(eventId));

		verify(syncService, timeout(FakeKafka.CONSUME_TIMEOUT.toMillis())).handleCatalogUpdated(eventId);
		assertOffsetConfirmed(sent);
	}

	@Test
	void stillTriggersTheCycleForAMalformedNoticeAndConfirmsTheOffset() {
		RecordMetadata sent = FakeKafka.publish(TOPIC, "8", "esto no es JSON");

		verify(syncService, timeout(FakeKafka.CONSUME_TIMEOUT.toMillis())).handleCatalogUpdated(null);
		assertOffsetConfirmed(sent);
	}

	@Test
	void confirmsTheOffsetEvenWhenProcessingFails() {
		String eventId = "catalog-event-" + UUID.randomUUID();
		doThrow(new IllegalStateException("la base no responde")).when(syncService).handleCatalogUpdated(eventId);

		RecordMetadata sent = FakeKafka.publish(TOPIC, "9", notice(eventId));

		verify(syncService, timeout(FakeKafka.CONSUME_TIMEOUT.toMillis())).handleCatalogUpdated(eventId);
		assertOffsetConfirmed(sent);
	}

	private void assertOffsetConfirmed(RecordMetadata sent) {
		await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
				assertThat(FakeKafka.committedOffset(GROUP, TOPIC, sent.partition())).isGreaterThan(sent.offset()));
	}

	private static String notice(String eventId) {
		return "{\"eventId\":\"" + eventId + "\",\"eventType\":\"CatalogUpdated\",\"newVersion\":7,"
				+ "\"occurredAt\":\"2026-09-10T13:44:59Z\",\"source\":\"MANUAL\",\"schemaVersion\":1}";
	}

}
