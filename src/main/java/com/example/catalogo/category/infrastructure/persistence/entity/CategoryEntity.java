package com.example.catalogo.category.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "professional_category")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryEntity {
	// Sin @GeneratedValue: el id lo asigna la cátedra y se guarda tal cual. Las fechas también
	// son las de la cátedra, por eso no hay auditoría automática como en los datos propios.
	@Id
	private Long id;
	private String name;
	private String description;
	private boolean enabled;
	private Instant createdAt;
	private Instant updatedAt;
}
