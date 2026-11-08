package com.wzabek.rentengine.catalog.mapper;

import com.wzabek.rentengine.catalog.dto.UnitResponse;
import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import org.springframework.stereotype.Component;

@Component
public class UnitMapper {

    public UnitResponse toResponse(EquipmentUnit unit) {
        return new UnitResponse(
                unit.getId(),
                unit.getEquipment().getId(),
                unit.getEquipment().getName(),
                unit.getSerialOrPlate(),
                unit.getStatus(),
                unit.getMileageOrHours()
        );
    }
}
