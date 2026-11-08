package com.wzabek.rentengine.settlement.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

// Czysta logika cenowa bez zależności od Springa, żeby łatwo ją testować.

public class PricingCalculator {

    private static final long MINUTES_PER_DAY = 24 * 60;

    // Każda rozpoczęta doba liczy się jako pełna: 24 h = 1 doba, 24 h 1 min = 2 doby
    public long rentalDays(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End must be after start");
        }
        long minutes = Duration.between(start, end).toMinutes();
        return Math.max(1, Math.ceilDiv(minutes, MINUTES_PER_DAY));
    }

    public BigDecimal basePrice(BigDecimal dailyPrice, LocalDateTime start, LocalDateTime end) {
        return dailyPrice.multiply(BigDecimal.valueOf(rentalDays(start, end)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
