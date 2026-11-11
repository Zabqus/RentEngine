package com.wzabek.rentengine.settlement.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties("app.pricing")
public record PricingProperties(
        // Cena litra paliwa doliczana za braki przy zwrocie (R4)
        @NotNull @PositiveOrZero BigDecimal fuelPrice
) {
}
