package com.example.catalogo.category.infrastructure.persistence.repository;

import java.util.List;

import com.example.catalogo.category.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCategoryRepository extends JpaRepository<CategoryEntity, Long> {

	List<CategoryEntity> findAllByEnabledTrueOrderByNameAsc();

}
