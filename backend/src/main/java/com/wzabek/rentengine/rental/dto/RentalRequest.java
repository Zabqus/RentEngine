package com.wzabek.rentengine.rental.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record RentalRequest(
        // TYMCZASOWO do Dnia 5: potem klient z tokena JWT, a to pole znika
        @NotNull @Positive Long clientId,
        @NotNull @Positive Long unitId,
        @NotNull @Future LocalDateTime startDate,
        @NotNull LocalDateTime endDate
) {
}
