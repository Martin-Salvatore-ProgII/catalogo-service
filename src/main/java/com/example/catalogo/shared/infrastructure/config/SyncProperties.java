package com.example.catalogo.shared.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Valores operativos de la sincronización (RNF-09). El intervalo del chequeo periódico lo lee
 * directamente {@code @Scheduled}; acá va lo que necesita el código.
 *
 * @param processedEventRetention cuánto se conservan los {@code eventId} de los avisos procesados
 *                                (ADR-0008)
 */
@ConfigurationProperties("catalogo.sync")
public record SyncProperties(Duration checkInterval, Duration processedEventRetention) {
}
