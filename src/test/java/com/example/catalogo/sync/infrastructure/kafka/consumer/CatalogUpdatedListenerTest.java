package com.example.catalogo.sync.infrastructure.kafka.consumer;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.example.catalogo.sync.application.service.SyncService;
import com.example.catalogo.sync.infrastructure.kafka.mapper.CatalogUpdatedMessageMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.json.JsonMapper;

// El adaptador no decide nada: lee, delega y confirma. Sin Spring ni broker.
@ExtendWith(MockitoExtension.class)
class CatalogUpdatedListenerTest {

	private static final String VALID = "{\"eventId\":\"e-1\",\"eventType\":\"CatalogUpdated\",\"newVersion\":7}";

	@Mock
	private SyncService syncService;
	@Mock
	private Acknowledgment acknowledgment;

	private final CatalogUpdatedMessageMapper mapper = new CatalogUpdatedMessageMapper(JsonMapper.builder().build());

	private CatalogUpdatedListener listener() {
		return new CatalogUpdatedListener(syncService, mapper);
	}

	@Test
	void passesTheEventIdToTheServiceAndOnlyThenConfirmsTheOffset() {
		listener().onCatalogUpdated(record(VALID), acknowledgment);

		InOrder order = inOrder(syncService, acknowledgment);
		order.verify(syncService).handleCatalogUpdated("e-1");
		order.verify(acknowledgment).acknowledge();
	}

	@Test
	void stillTriggersTheCycleWhenTheNoticeIsMalformed() {
		// CU-01, 1b.
		listener().onCatalogUpdated(record("basura"), acknowledgment);

		verify(syncService).handleCatalogUpdated(null);
		verify(acknowledgment).acknowledge();
	}

	@Test
	void confirmsTheOffsetEvenWhenTheNoticeCannotBeProcessed() {
		// ADR-0011: sin confirmar, Kafka reentregaría el mismo aviso mientras dure la falla.
		doThrow(new IllegalStateException("la base no responde")).when(syncService).handleCatalogUpdated("e-1");

		listener().onCatalogUpdated(record(VALID), acknowledgment);

		verify(acknowledgment).acknowledge();
	}

	private static ConsumerRecord<String, String> record(String value) {
		return new ConsumerRecord<>("catedra.catalog.proyecto-de-prueba", 0, 12L, "7", value);
	}

}
