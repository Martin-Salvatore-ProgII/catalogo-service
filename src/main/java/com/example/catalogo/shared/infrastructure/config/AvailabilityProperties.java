package com.example.catalogo.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Horizonte de fechas del filtro de disponibilidad (ADR-0015, ADR-0065, RNF-09). Tiene que valer lo
// mismo que el de turnos.
@ConfigurationProperties("catalogo.availability")
public record AvailabilityProperties(int horizonDays) {
}
