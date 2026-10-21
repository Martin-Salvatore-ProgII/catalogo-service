package com.example.catalogo.professional.domain.model;

import java.time.ZoneId;

// Zona horaria de la agenda: fija y no configurable (ADR-0016). Con ella se decide qué día es "hoy".
public final class AgendaZone {

	public static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

	private AgendaZone() {
	}

}
