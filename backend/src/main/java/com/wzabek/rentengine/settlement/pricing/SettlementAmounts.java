package com.wzabek.rentengine.settlement.pricing;

import java.math.BigDecimal;

// total = baza + kara + paliwo + nadwyżka szkód ponad kaucję
public record SettlementAmounts(
        BigDecimal baseAmount,
        long lateDays,
        BigDecimal lateFee,
        BigDecimal fuelFee,
        BigDecimal damageFee,
        BigDecimal depositDeduction,
        BigDecimal total
) {
}
