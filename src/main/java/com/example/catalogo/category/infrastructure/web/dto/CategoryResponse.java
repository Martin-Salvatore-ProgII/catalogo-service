package com.example.catalogo.category.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Solo lo que define el contrato. El estado y las fechas de la cátedra no se exponen: la app
// recibe únicamente categorías habilitadas y no necesita saber cuándo cambiaron.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
	private Long id;
	private String name;
	private String description;
}
