package com.wzabek.rentengine.rental.service;

import com.wzabek.rentengine.catalog.entity.Equipment;
import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import com.wzabek.rentengine.catalog.entity.UnitStatus;
import com.wzabek.rentengine.catalog.repository.EquipmentUnitRepository;
import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.common.exception.InvalidRequestException;
import com.wzabek.rentengine.common.exception.NotFoundException;
import com.wzabek.rentengine.rental.dto.RentalRequest;
import com.wzabek.rentengine.rental.dto.RentalResponse;
import com.wzabek.rentengine.rental.entity.AgreementStatus;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.rental.mapper.RentalMapper;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import com.wzabek.rentengine.settlement.pricing.PricingCalculator;
import com.wzabek.rentengine.settlement.repository.SettlementRepository;
import com.wzabek.rentengine.user.entity.User;
import com.wzabek.rentengine.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentalService {

    private final RentalAgreementRepository agreementRepository;
    private final EquipmentUnitRepository unitRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final PricingCalculator pricingCalculator;
    private final RentalMapper mapper;

    public RentalResponse findById(Long id) {
        return mapper.toResponse(getAgreement(id));
    }

    @Transactional
    public RentalResponse reserve(RentalRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new InvalidRequestException(Map.of("endDate", "must be after startDate"));
        }
        User client = userRepository.findById(request.clientId())
                .orElseThrow(() -> new NotFoundException("User", request.clientId()));
        EquipmentUnit unit = unitRepository.findByIdForReservation(request.unitId())
                .orElseThrow(() -> new NotFoundException("Unit", request.unitId()));

        // R6
        if (settlementRepository.existsByAgreementClientIdAndPaidFalse(client.getId())) {
            throw new BusinessRuleException("UNPAID_SETTLEMENT",
                    "Client has an unpaid settlement and cannot make new reservations");
        }
        // R2
        if (unit.getStatus() == UnitStatus.IN_SERVICE) {
            throw new BusinessRuleException("UNIT_IN_SERVICE",
                    "Unit '%s' is in service".formatted(unit.getSerialOrPlate()));
        }
        // R1
        if (agreementRepository.existsOverlapping(unit.getId(), request.startDate(), request.endDate())) {
            throw new BusinessRuleException("UNIT_NOT_AVAILABLE",
                    "Unit '%s' is already booked in this period".formatted(unit.getSerialOrPlate()));
        }

        Equipment equipment = unit.getEquipment();
        BigDecimal basePrice = pricingCalculator.basePrice(equipment.getDailyPrice(),
                request.startDate(), request.endDate());
        RentalAgreement agreement = new RentalAgreement(client, unit, request.startDate(), request.endDate(),
                basePrice, equipment.getDeposit());
        return mapper.toResponse(agreementRepository.save(agreement));
    }

    // R8 pilnuje, że anulować można tylko RESERVED. Sprawdzenie właściciela dojdzie w Dniu 5.
    @Transactional
    public RentalResponse cancel(Long id) {
        RentalAgreement agreement = getAgreement(id);
        agreement.changeStatus(AgreementStatus.CANCELLED);
        return mapper.toResponse(agreement);
    }

    public RentalAgreement getAgreement(Long id) {
        return agreementRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Rental agreement", id));
    }
}
