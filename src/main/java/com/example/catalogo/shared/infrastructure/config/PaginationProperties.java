package com.example.catalogo.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Tamaño de página por defecto y máximo de las listas paginadas (docs/arq/contratos.md, RNF-09).
@ConfigurationProperties("catalogo.pagination")
public record PaginationProperties(int defaultSize, int maxSize) {
}
