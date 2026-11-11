package com.wzabek.rentengine.settlement.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

// Czysta logika cenowa bez zależności od Springa, żeby łatwo ją testować.

public class PricingCalculator {

    private static final long MINUTES_PER_DAY = 24 * 60;
    private static final BigDecimal LATE_FEE_RATE = new BigDecimal("0.10");

    // Każda rozpoczęta doba liczy się jako pełna: 24 h = 1 doba, 24 h 1 min = 2 doby
    public long rentalDays(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End must be after start");
        }
        long minutes = Duration.between(start, end).toMinutes();
        return Math.max(1, Math.ceilDiv(minutes, MINUTES_PER_DAY));
    }

    public BigDecimal basePrice(BigDecimal dailyPrice, LocalDateTime start, LocalDateTime end) {
        return money(dailyPrice.multiply(BigDecimal.valueOf(rentalDays(start, end))));
    }

    // Rozpoczęte doby spóźnienia; pełne minuty, więc zwrot w tej samej minucie co termin jest jeszcze na czas
    public long lateDays(LocalDateTime endDate, LocalDateTime returnedAt) {
        long minutes = Duration.between(endDate, returnedAt).toMinutes();
        return minutes <= 0 ? 0 : Math.ceilDiv(minutes, MINUTES_PER_DAY);
    }

    // R3: +10% stawki dobowej za każdą rozpoczętą dobę spóźnienia
    public BigDecimal lateFee(BigDecimal dailyRate, LocalDateTime endDate, LocalDateTime returnedAt) {
        return money(dailyRate.multiply(LATE_FEE_RATE).multiply(BigDecimal.valueOf(lateDays(endDate, returnedAt))));
    }

    // R4: brakujące litry względem wydania × cena; nadmiar paliwa nie daje zwrotu
    public BigDecimal fuelFee(BigDecimal issuedLiters, BigDecimal returnedLiters, BigDecimal pricePerLiter) {
        if (issuedLiters == null || returnedLiters == null) {
            return money(BigDecimal.ZERO);
        }
        BigDecimal missing = issuedLiters.subtract(returnedLiters).max(BigDecimal.ZERO);
        return money(missing.multiply(pricePerLiter));
    }

    public SettlementAmounts settle(SettlementInput in) {
        // Stawka z umowy, a nie z cennika, żeby zmiana cen nie ruszała zawartych umów
        BigDecimal dailyRate = in.basePrice()
                .divide(BigDecimal.valueOf(rentalDays(in.startDate(), in.endDate())), 2, RoundingMode.HALF_UP);
        BigDecimal base = money(in.basePrice());
        BigDecimal lateFee = lateFee(dailyRate, in.endDate(), in.returnedAt());
        BigDecimal fuelFee = fuelFee(in.issuedFuel(), in.returnedFuel(), in.fuelPrice());

        // R5: szkody najpierw z kaucji, nadwyżka do rachunku
        BigDecimal damageCost = money(in.damageCost());
        BigDecimal depositDeduction = damageCost.min(money(in.deposit()));
        BigDecimal damageFee = damageCost.subtract(depositDeduction);

        BigDecimal total = base.add(lateFee).add(fuelFee).add(damageFee);
        return new SettlementAmounts(base, lateDays(in.endDate(), in.returnedAt()), lateFee, fuelFee,
                damageFee, depositDeduction, total);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
