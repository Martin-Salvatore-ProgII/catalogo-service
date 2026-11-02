package com.example.catalogo.sync.infrastructure.kafka.mapper;

import com.example.catalogo.sync.infrastructure.kafka.dto.CatalogUpdatedMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class CatalogUpdatedMessageMapper {

	static final String EVENT_TYPE = "CatalogUpdated";

	private final JsonMapper jsonMapper;

	// El eventId del aviso, o null si no se puede leer: JSON ilegible, otro eventType o sin eventId
	// (CU-01, 1b). Nunca lanza: un aviso mal formado no puede frenar el consumo (ADR-0031).
	public String toEventId(String payload) {
		if (payload == null) {
			return null;
		}
		try {
			CatalogUpdatedMessage message = jsonMapper.readValue(payload, CatalogUpdatedMessage.class);
			boolean valid = message != null && EVENT_TYPE.equals(message.getEventType())
					&& message.getEventId() != null && !message.getEventId().isBlank();
			return valid ? message.getEventId() : null;
		} catch (JacksonException e) {
			return null;
		}
	}

}
