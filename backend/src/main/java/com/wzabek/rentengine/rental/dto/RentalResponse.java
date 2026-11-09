package com.wzabek.rentengine.rental.dto;

import com.wzabek.rentengine.rental.entity.AgreementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RentalResponse(
        Long id,
        Long clientId,
        String clientName,
        Long unitId,
        String serialOrPlate,
        Long equipmentId,
        String equipmentName,
        LocalDateTime startDate,
        LocalDateTime endDate,
        AgreementStatus status,
        BigDecimal basePrice,
        BigDecimal deposit,
        LocalDateTime createdAt
) {
}
