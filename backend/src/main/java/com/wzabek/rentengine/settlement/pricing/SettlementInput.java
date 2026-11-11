package com.wzabek.rentengine.settlement.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// issuedFuel/returnedFuel są null dla sprzętu bez zbiornika
public record SettlementInput(
        BigDecimal basePrice,
        BigDecimal deposit,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime returnedAt,
        BigDecimal issuedFuel,
        BigDecimal returnedFuel,
        BigDecimal fuelPrice,
        BigDecimal damageCost
) {
}
