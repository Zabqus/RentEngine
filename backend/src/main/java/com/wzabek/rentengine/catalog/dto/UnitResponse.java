package com.wzabek.rentengine.catalog.dto;

import com.wzabek.rentengine.catalog.entity.UnitStatus;

public record UnitResponse(
        Long id,
        Long equipmentId,
        String equipmentName,
        String serialOrPlate,
        UnitStatus status,
        int mileageOrHours
) {
}
