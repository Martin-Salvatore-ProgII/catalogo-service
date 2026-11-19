package com.example.catalogo.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties("catalogo.security.jwt")
public record JwtProperties(Resource publicKeyLocation) {
}   
// record es un tipo de clase contenedora de datos inmutables . 
// Resource es una abstraccion para acceder a recursos/archivos externos

// publickeylocation es leida por el application.yaml que antes fue definida en el compose.yaml