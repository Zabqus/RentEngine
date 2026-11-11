package com.wzabek.rentengine.protocol.dto;

import com.wzabek.rentengine.protocol.entity.DamageSeverity;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DamageRequest(
        @NotBlank @Size(max = 500) String description,
        @NotNull DamageSeverity severity,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal cost
) {
}
