package com.wzabek.rentengine.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UnitRequest(
        @NotNull @Positive Long equipmentId,
        @NotBlank @Size(max = 50) String serialOrPlate,
        @NotNull @PositiveOrZero Integer mileageOrHours
) {
}
