package com.wzabek.rentengine.rental.dto;

import com.wzabek.rentengine.rental.entity.AgreementStatus;
import jakarta.validation.constraints.Positive;

// Parametry GET /api/rentals; wszystkie opcjonalne
public record RentalFilter(
        AgreementStatus status,
        @Positive Long clientId,
        @Positive Long unitId
) {
}
