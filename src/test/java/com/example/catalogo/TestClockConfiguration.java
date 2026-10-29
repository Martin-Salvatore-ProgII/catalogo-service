package com.example.catalogo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Fija la hora de la aplicación completa, para las pruebas que dependen de "hoy" (docs/arq/pruebas.md:
 * pruebas deterministas, sin depender de la hora de la máquina). Es el martes 20 de octubre de 2026 a
 * las 12 de Argentina. Reemplaza al reloj real porque es {@code @Primary}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfiguration {

	public static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");
	public static final LocalDate TODAY = LocalDate.parse("2026-10-20");

	@Bean
	@Primary
	public Clock fixedClock() {
		return Clock.fixed(NOW, ZoneOffset.UTC);
	}

}
