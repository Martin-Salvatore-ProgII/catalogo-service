package com.example.catalogo.sync.domain.ports.in;

public interface RegisterProcessedEventUseCase {

	// true si el evento es nuevo y quedó registrado; false si ya se había procesado (ADR-0008).
	boolean registerProcessedEvent(String eventId);

}
