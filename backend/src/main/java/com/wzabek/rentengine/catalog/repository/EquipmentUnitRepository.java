package com.wzabek.rentengine.catalog.repository;

import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EquipmentUnitRepository extends JpaRepository<EquipmentUnit, Long> {

    @EntityGraph(attributePaths = "equipment")
    List<EquipmentUnit> findByEquipmentId(Long equipmentId, Sort sort);

    @Override
    @EntityGraph(attributePaths = "equipment")
    List<EquipmentUnit> findAll(Sort sort);

    boolean existsByEquipmentId(Long equipmentId);

    // Jawne zapytania, bo w nazwie metody "Or" zostałoby odczytane jako operator OR
    @Query("select count(u) > 0 from EquipmentUnit u where u.serialOrPlate = :serialOrPlate")
    boolean existsBySerialOrPlateValue(String serialOrPlate);

    @Query("select count(u) > 0 from EquipmentUnit u where u.serialOrPlate = :serialOrPlate and u.id <> :id")
    boolean existsBySerialOrPlateValueAndIdNot(String serialOrPlate, Long id);
}
