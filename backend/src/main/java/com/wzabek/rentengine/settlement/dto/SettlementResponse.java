package com.wzabek.rentengine.settlement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// damageCost = depositDeduction + damageFee; depositRefund = kaucja do oddania klientowi
public record SettlementResponse(
        Long id,
        Long agreementId,
        BigDecimal baseAmount,
        long lateDays,
        BigDecimal lateFee,
        BigDecimal fuelFee,
        BigDecimal damageCost,
        BigDecimal depositDeduction,
        BigDecimal damageFee,
        BigDecimal deposit,
        BigDecimal depositRefund,
        BigDecimal total,
        boolean paid,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
}
