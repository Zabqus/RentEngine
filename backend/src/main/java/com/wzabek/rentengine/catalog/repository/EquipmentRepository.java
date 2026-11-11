package com.wzabek.rentengine.catalog.repository;

import com.wzabek.rentengine.catalog.entity.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {

    boolean existsByCategoryId(Long categoryId);

    List<Equipment> findByCategoryId(Long categoryId);

    // Kategoria w jednym zapytaniu, bez N+1 przy mapowaniu strony
    @Override
    @EntityGraph(attributePaths = "category")
    Page<Equipment> findAll(Specification<Equipment> spec, Pageable pageable);
}
