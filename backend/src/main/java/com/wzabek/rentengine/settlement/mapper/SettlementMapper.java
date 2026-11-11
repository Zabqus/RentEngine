package com.wzabek.rentengine.settlement.mapper;

import com.wzabek.rentengine.settlement.dto.SettlementResponse;
import com.wzabek.rentengine.settlement.entity.Settlement;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SettlementMapper {

    // Dni spóźnienia nie są w tabeli, więc liczy je serwis
    public SettlementResponse toResponse(Settlement settlement, long lateDays) {
        BigDecimal deposit = settlement.getAgreement().getDeposit();
        return new SettlementResponse(
                settlement.getId(),
                settlement.getAgreement().getId(),
                settlement.getBaseAmount(),
                lateDays,
                settlement.getLateFee(),
                settlement.getFuelFee(),
                settlement.getDepositDeduction().add(settlement.getDamageFee()),
                settlement.getDepositDeduction(),
                settlement.getDamageFee(),
                deposit,
                deposit.subtract(settlement.getDepositDeduction()),
                settlement.getTotal(),
                settlement.isPaid(),
                settlement.getCreatedAt(),
                settlement.getPaidAt()
        );
    }
}
