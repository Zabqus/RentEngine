package com.wzabek.rentengine.catalog.mapper;

import com.wzabek.rentengine.catalog.dto.EquipmentResponse;
import com.wzabek.rentengine.catalog.entity.Equipment;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Component
public class EquipmentMapper {

    public EquipmentResponse toResponse(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getName(),
                equipment.getCategory().getId(),
                equipment.getCategory().getName(),
                equipment.getCategory().getType(),
                equipment.getDailyPrice(),
                equipment.getDeposit(),
                equipment.getTankCapacity(),
                new LinkedHashMap<>(equipment.getParams())
        );
    }
}
