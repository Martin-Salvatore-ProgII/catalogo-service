package com.example.catalogo.shared.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("catalogo.security.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
