package com.example.catalogo.sync.infrastructure.kafka.consumer;

import com.example.catalogo.sync.application.service.SyncService;
import com.example.catalogo.sync.infrastructure.kafka.mapper.CatalogUpdatedMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Disparador por aviso de Kafka (ADR-0006). Kafka es solo una notificación: los datos salen de
 * Redis o del snapshot. El topic sale de la integración que se pidió al arrancar (ADR-0005).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogUpdatedListener {

	private final SyncService syncService;
	private final CatalogUpdatedMessageMapper messageMapper;

	@KafkaListener(topics = "#{@catedraIntegration.kafkaCatalogTopic()}")
	public void onCatalogUpdated(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
		try {
			syncService.handleCatalogUpdated(messageMapper.toEventId(record.value()));
		} catch (RuntimeException e) {
			// Por ejemplo, la base no responde al registrar el aviso. No se reintenta acá ni se deja
			// el offset sin confirmar: Kafka reentregaría el mismo aviso mientras dure la falla. El
			// chequeo periódico lo recupera (ADR-0011).
			log.error("CatalogUpdated notice at offset {} could not be processed", record.offset(), e);
		}
		// Recién ahora, con el ciclo terminado y su resultado ya guardado (REF §16). El caso de uso
		// no lanza ante una falla de la cátedra: la registra en el estado de sincronización.
		acknowledgment.acknowledge();
	}

}
