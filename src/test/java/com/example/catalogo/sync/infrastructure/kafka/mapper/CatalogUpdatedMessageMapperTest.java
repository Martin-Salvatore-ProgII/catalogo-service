package com.example.catalogo.sync.infrastructure.kafka.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

// Sin Spring. El mensaje de ejemplo es el de la REF §15.3.
class CatalogUpdatedMessageMapperTest {

	private final CatalogUpdatedMessageMapper mapper = new CatalogUpdatedMessageMapper(JsonMapper.builder().build());

	@Test
	void readsTheEventIdOfAValidNotice() {
		String payload = """
				{"eventId":"catalog-event-7-4f3b","eventType":"CatalogUpdated","newVersion":7,
				 "occurredAt":"2026-09-10T13:44:59Z","source":"MANUAL","schemaVersion":1}
				""";

		assertThat(mapper.toEventId(payload)).isEqualTo("catalog-event-7-4f3b");
	}

	@Test
	void ignoresUnknownFieldsCompatibleWithV1() {
		String payload = "{\"eventId\":\"e-1\",\"eventType\":\"CatalogUpdated\",\"campoNuevo\":{\"a\":1}}";

		assertThat(mapper.toEventId(payload)).isEqualTo("e-1");
	}

	@Test
	void answersNullForTextThatIsNotJson() {
		assertThat(mapper.toEventId("esto no es JSON")).isNull();
	}

	@Test
	void answersNullForAnEmptyOrMissingPayload() {
		assertThat(mapper.toEventId("")).isNull();
		assertThat(mapper.toEventId(null)).isNull();
	}

	@Test
	void answersNullWhenTheEventIdIsMissingOrBlank() {
		assertThat(mapper.toEventId("{\"eventType\":\"CatalogUpdated\",\"newVersion\":7}")).isNull();
		assertThat(mapper.toEventId("{\"eventId\":\" \",\"eventType\":\"CatalogUpdated\"}")).isNull();
	}

	@Test
	void answersNullForAnotherEventType() {
		assertThat(mapper.toEventId("{\"eventId\":\"e-1\",\"eventType\":\"AppointmentConfirmed\"}")).isNull();
	}

	@Test
	void answersNullWhenTheFieldsHaveTheWrongType() {
		assertThat(mapper.toEventId("{\"eventId\":[1,2],\"eventType\":\"CatalogUpdated\"}")).isNull();
	}

}
