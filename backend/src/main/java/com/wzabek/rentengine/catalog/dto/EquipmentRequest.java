package com.wzabek.rentengine.catalog.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Map;

public record EquipmentRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull @Positive Long categoryId,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal dailyPrice,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal deposit,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal tankCapacity,
        @NotNull Map<String, Object> params
) {
}
