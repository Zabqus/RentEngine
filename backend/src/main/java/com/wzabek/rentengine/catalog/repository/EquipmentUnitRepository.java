package com.wzabek.rentengine.catalog.repository;

import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EquipmentUnitRepository extends JpaRepository<EquipmentUnit, Long> {

    List<EquipmentUnit> findByEquipmentId(Long equipmentId);

    boolean existsByEquipmentId(Long equipmentId);

    // Jawne zapytanie, bo w nazwie metody "Or" zostałoby odczytane jako operator OR
    @Query("select count(u) > 0 from EquipmentUnit u where u.serialOrPlate = :serialOrPlate")
    boolean existsBySerialOrPlateValue(String serialOrPlate);
}
