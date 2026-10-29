package com.example.catalogo.professional.infrastructure.persistence.entity;

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
@Table(name = "professional")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalEntity {
	// Sin @GeneratedValue: el id lo asigna la cátedra y se guarda tal cual. Las fechas también
	// son las de la cátedra, por eso no hay auditoría automática como en los datos propios.
	@Id
	private Long id;
	// La categoría es otra feature: se guarda su id, como lo publica la cátedra, y no una relación
	// JPA a CategoryEntity (ADR-0062). La integridad la garantiza la clave foránea de la base.
	private Long categoryId;
	private String firstName;
	private String lastName;
	private boolean enabled;
	private Instant createdAt;
	private Instant updatedAt;
}
