package com.wzabek.rentengine.catalog.repository;

import com.wzabek.rentengine.catalog.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {

    boolean existsByCategoryId(Long categoryId);

    List<Equipment> findByCategoryId(Long categoryId);
}
