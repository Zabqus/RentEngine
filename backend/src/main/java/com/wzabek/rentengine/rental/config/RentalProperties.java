package com.wzabek.rentengine.rental.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties("app.rental")
public record RentalProperties(
        // Ile czasu po starcie czekamy na odbiór, zanim scheduler anuluje rezerwację
        @NotNull Duration pickupGrace
) {
}
