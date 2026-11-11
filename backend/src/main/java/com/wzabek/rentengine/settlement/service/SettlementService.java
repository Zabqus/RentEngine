package com.wzabek.rentengine.settlement.service;

import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.common.exception.NotFoundException;
import com.wzabek.rentengine.protocol.entity.Protocol;
import com.wzabek.rentengine.protocol.entity.ProtocolType;
import com.wzabek.rentengine.protocol.repository.DamageRepository;
import com.wzabek.rentengine.protocol.repository.ProtocolRepository;
import com.wzabek.rentengine.rental.entity.AgreementStatus;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.rental.service.RentalService;
import com.wzabek.rentengine.settlement.config.PricingProperties;
import com.wzabek.rentengine.settlement.dto.SettlementResponse;
import com.wzabek.rentengine.settlement.entity.Settlement;
import com.wzabek.rentengine.settlement.mapper.SettlementMapper;
import com.wzabek.rentengine.settlement.pricing.PricingCalculator;
import com.wzabek.rentengine.settlement.pricing.SettlementAmounts;
import com.wzabek.rentengine.settlement.pricing.SettlementInput;
import com.wzabek.rentengine.settlement.repository.SettlementRepository;
import com.wzabek.rentengine.user.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final ProtocolRepository protocolRepository;
    private final DamageRepository damageRepository;
    private final RentalService rentalService;
    private final PricingCalculator pricingCalculator;
    private final PricingProperties pricingProperties;
    private final SettlementMapper mapper;
    private final Clock clock;

    // Rozliczenie liczymy raz, po zwrocie; potem kwoty są zamrożone (uszkodzeń nie da się już dodać)
    @Transactional
    public SettlementResponse create(Long agreementId) {
        RentalAgreement agreement = rentalService.getAgreement(agreementId);
        if (settlementRepository.existsByAgreementId(agreementId)) {
            throw new BusinessRuleException("SETTLEMENT_EXISTS",
                    "Settlement for this agreement already exists");
        }
        if (agreement.getStatus() != AgreementStatus.RETURNED) {
            throw new BusinessRuleException("AGREEMENT_NOT_RETURNED",
                    "Settlement can be created only for a returned agreement (current status: %s)"
                            .formatted(agreement.getStatus()));
        }

        Protocol issue = getProtocol(agreementId, ProtocolType.ISSUE);
        Protocol returned = getProtocol(agreementId, ProtocolType.RETURN);
        SettlementAmounts amounts = pricingCalculator.settle(new SettlementInput(
                agreement.getBasePrice(),
                agreement.getDeposit(),
                agreement.getStartDate(),
                agreement.getEndDate(),
                returned.getCreatedAt(),
                issue.getFuelLevel(),
                returned.getFuelLevel(),
                pricingProperties.fuelPrice(),
                // Tylko szkody ze zwrotu; te z wydania opisują stan zastany (R5)
                damageRepository.sumCost(agreementId, ProtocolType.RETURN)
        ));

        Settlement settlement = settlementRepository.save(new Settlement(agreement, amounts.baseAmount(),
                amounts.lateFee(), amounts.fuelFee(), amounts.damageFee(), amounts.depositDeduction(), amounts.total()));
        return mapper.toResponse(settlement, amounts.lateDays());
    }

    public SettlementResponse findByAgreement(Long agreementId, AuthenticatedUser user) {
        rentalService.getAccessibleAgreement(agreementId, user);
        Settlement settlement = settlementRepository.findByAgreementId(agreementId)
                .orElseThrow(() -> new NotFoundException("Settlement for rental agreement", agreementId));
        return toResponse(settlement);
    }

    // Opłacenie zamyka umowę (RETURNED → SETTLED) i zdejmuje blokadę R6
    @Transactional
    public SettlementResponse pay(Long id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Settlement", id));
        if (settlement.isPaid()) {
            throw new BusinessRuleException("SETTLEMENT_ALREADY_PAID", "Settlement is already paid");
        }
        settlement.markPaid(LocalDateTime.now(clock));
        settlement.getAgreement().changeStatus(AgreementStatus.SETTLED);
        return toResponse(settlement);
    }

    private SettlementResponse toResponse(Settlement settlement) {
        RentalAgreement agreement = settlement.getAgreement();
        Protocol returned = getProtocol(agreement.getId(), ProtocolType.RETURN);
        return mapper.toResponse(settlement, pricingCalculator.lateDays(agreement.getEndDate(), returned.getCreatedAt()));
    }

    // Umowa RETURNED zawsze ma oba protokoły, więc brak to błąd danych
    private Protocol getProtocol(Long agreementId, ProtocolType type) {
        return protocolRepository.findByAgreementIdAndType(agreementId, type)
                .orElseThrow(() -> new IllegalStateException(
                        "Agreement %d has no %s protocol".formatted(agreementId, type)));
    }
}
