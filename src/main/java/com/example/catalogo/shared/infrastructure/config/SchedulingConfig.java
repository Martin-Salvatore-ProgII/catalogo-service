package com.example.catalogo.shared.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Habilita los disparadores por tiempo (@Scheduled), como el chequeo periódico de sincronización.
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
