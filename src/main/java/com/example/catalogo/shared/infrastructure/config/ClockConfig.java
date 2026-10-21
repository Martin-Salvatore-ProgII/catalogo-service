package com.example.catalogo.shared.infrastructure.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// La hora se inyecta, para que las pruebas la puedan fijar (docs/arq/pruebas.md). Los instantes se
// guardan en UTC (REF §4).
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
