package com.example.catalogo.professional.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// La categoría de un profesional, solo con lo que la app muestra (esquema CategoryRef del contrato).
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRefResponse {
	private Long id;
	private String name;
}
