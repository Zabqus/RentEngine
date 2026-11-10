package com.wzabek.rentengine.rental.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

// Klient nie jest w body: bierzemy go z tokena JWT
public record RentalRequest(
        @NotNull @Positive Long unitId,
        @NotNull @Future LocalDateTime startDate,
        @NotNull LocalDateTime endDate
) {
}
