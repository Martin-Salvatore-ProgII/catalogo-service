package com.example.catalogo.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties("catalogo.security.jwt")
public record JwtProperties(Resource publicKeyLocation) {
}
