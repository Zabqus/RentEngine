package com.wzabek.rentengine.catalog.repository;

import com.wzabek.rentengine.catalog.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EquipmentRepository extends JpaRepository<Equipment, Long>, JpaSpecificationExecutor<Equipment> {

    boolean existsByCategoryId(Long categoryId);
}
