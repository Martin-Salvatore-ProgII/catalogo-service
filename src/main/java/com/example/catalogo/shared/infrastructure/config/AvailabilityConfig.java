package com.example.catalogo.shared.infrastructure.config;

import com.example.catalogo.professional.domain.model.AvailabilityHorizon;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Pasa el horizonte configurado al dominio, como un valor propio y no como la clase de configuración.
@Configuration
@EnableConfigurationProperties(AvailabilityProperties.class)
public class AvailabilityConfig {

	@Bean
	public AvailabilityHorizon availabilityHorizon(AvailabilityProperties properties) {
		return AvailabilityHorizon.builder().days(properties.horizonDays()).build();
	}

}
