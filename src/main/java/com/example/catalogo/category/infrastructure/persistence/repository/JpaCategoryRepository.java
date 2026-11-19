package com.example.catalogo.category.infrastructure.persistence.repository;

import java.util.List;

import com.example.catalogo.category.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// extienda spring-data-JPA y a la vez es la interfaz del Adaptador JpaCategoryRepository
// es quien genera el codigo SQL
public interface JpaCategoryRepository extends JpaRepository<CategoryEntity, Long> {

	List<CategoryEntity> findAllByEnabledTrueOrderByNameAsc();

}
