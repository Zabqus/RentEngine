package com.wzabek.rentengine.protocol.dto;

import com.wzabek.rentengine.protocol.entity.ProtocolType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProtocolResponse(
        Long id,
        Long agreementId,
        ProtocolType type,
        BigDecimal fuelLevel,
        int mileageOrHours,
        String notes,
        Long employeeId,
        String employeeName,
        LocalDateTime createdAt,
        List<DamageResponse> damages
) {
}
