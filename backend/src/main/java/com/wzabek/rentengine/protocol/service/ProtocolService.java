package com.wzabek.rentengine.protocol.service;

import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import com.wzabek.rentengine.catalog.entity.UnitStatus;
import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.common.exception.InvalidRequestException;
import com.wzabek.rentengine.common.exception.NotFoundException;
import com.wzabek.rentengine.protocol.dto.DamageRequest;
import com.wzabek.rentengine.protocol.dto.DamageResponse;
import com.wzabek.rentengine.protocol.dto.ProtocolRequest;
import com.wzabek.rentengine.protocol.dto.ProtocolResponse;
import com.wzabek.rentengine.protocol.entity.Damage;
import com.wzabek.rentengine.protocol.entity.DamageSeverity;
import com.wzabek.rentengine.protocol.entity.Protocol;
import com.wzabek.rentengine.protocol.entity.ProtocolType;
import com.wzabek.rentengine.protocol.mapper.ProtocolMapper;
import com.wzabek.rentengine.protocol.repository.DamageRepository;
import com.wzabek.rentengine.protocol.repository.ProtocolRepository;
import com.wzabek.rentengine.rental.entity.AgreementStatus;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import com.wzabek.rentengine.rental.service.RentalService;
import com.wzabek.rentengine.settlement.repository.SettlementRepository;
import com.wzabek.rentengine.user.entity.User;
import com.wzabek.rentengine.user.repository.UserRepository;
import com.wzabek.rentengine.user.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProtocolService {

    private static final List<AgreementStatus> RENTED_OUT = List.of(AgreementStatus.ACTIVE, AgreementStatus.OVERDUE);

    private final ProtocolRepository protocolRepository;
    private final DamageRepository damageRepository;
    private final RentalAgreementRepository agreementRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final RentalService rentalService;
    private final ProtocolMapper mapper;
    private final Clock clock;

    public List<ProtocolResponse> findByAgreement(Long agreementId, AuthenticatedUser user) {
        rentalService.getAccessibleAgreement(agreementId, user);
        return protocolRepository.findByAgreementIdOrderByCreatedAtAsc(agreementId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    // Wydanie: RESERVED → ACTIVE, zwrot: ACTIVE/OVERDUE → RETURNED (R8 pilnuje kolejności i duplikatów)
    @Transactional
    public ProtocolResponse create(Long agreementId, ProtocolRequest request, Long employeeId) {
        RentalAgreement agreement = rentalService.getAgreement(agreementId);
        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("User", employeeId));
        EquipmentUnit unit = agreement.getUnit();

        if (request.type() == ProtocolType.ISSUE) {
            agreement.changeStatus(AgreementStatus.ACTIVE);
            checkCanIssue(agreement, unit);
        } else {
            agreement.changeStatus(AgreementStatus.RETURNED);
        }
        validateReadings(request, unit);
        unit.setMileageOrHours(request.mileageOrHours());

        Protocol protocol = new Protocol(agreement, request.type(), scale(request.fuelLevel()),
                request.mileageOrHours(), normalizeNotes(request.notes()), employee);
        return mapper.toResponse(protocolRepository.save(protocol));
    }

    @Transactional
    public DamageResponse addDamage(Long protocolId, DamageRequest request) {
        Protocol protocol = protocolRepository.findById(protocolId)
                .orElseThrow(() -> new NotFoundException("Protocol", protocolId));
        RentalAgreement agreement = protocol.getAgreement();

        // Po rozliczeniu kwoty są zamknięte, nowe szkody by je rozjechały
        if (settlementRepository.existsByAgreementId(agreement.getId())) {
            throw new BusinessRuleException("AGREEMENT_SETTLED",
                    "Settlement for this agreement already exists, damages cannot be added");
        }
        // Sprzęt z poważnym uszkodzeniem w ogóle nie powinien zostać wydany
        if (protocol.getType() == ProtocolType.ISSUE && request.severity() == DamageSeverity.MAJOR) {
            throw new BusinessRuleException("MAJOR_DAMAGE_ON_ISSUE",
                    "Major damage cannot be recorded on an issue protocol");
        }

        Damage damage = new Damage(request.description().trim(), request.severity(), scale(request.cost()));
        protocol.addDamage(damage);
        damageRepository.save(damage);

        // R7
        if (request.severity() == DamageSeverity.MAJOR) {
            agreement.getUnit().setStatus(UnitStatus.IN_SERVICE);
        }
        return mapper.toResponse(damage);
    }

    private void checkCanIssue(RentalAgreement agreement, EquipmentUnit unit) {
        // R2: rezerwacja mogła powstać, zanim sprzęt trafił do serwisu
        if (unit.getStatus() == UnitStatus.IN_SERVICE) {
            throw new BusinessRuleException("UNIT_IN_SERVICE",
                    "Unit '%s' is in service and cannot be issued".formatted(unit.getSerialOrPlate()));
        }
        if (agreementRepository.existsByUnitIdAndStatusInAndIdNot(unit.getId(), RENTED_OUT, agreement.getId())) {
            throw new BusinessRuleException("UNIT_RENTED_OUT",
                    "Unit '%s' has not been returned from a previous rental yet".formatted(unit.getSerialOrPlate()));
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.toLocalDate().isBefore(agreement.getStartDate().toLocalDate())) {
            throw new BusinessRuleException("TOO_EARLY_TO_ISSUE",
                    "Unit can be issued on the start day (%s) at the earliest"
                            .formatted(agreement.getStartDate().toLocalDate()));
        }
        if (!now.isBefore(agreement.getEndDate())) {
            throw new BusinessRuleException("RENTAL_PERIOD_ENDED", "Rental period has already ended");
        }
    }

    private static void validateReadings(ProtocolRequest request, EquipmentUnit unit) {
        Map<String, String> errors = new LinkedHashMap<>();
        BigDecimal tank = unit.getEquipment().getTankCapacity();
        BigDecimal fuel = request.fuelLevel();
        if (tank == null && fuel != null) {
            errors.put("fuelLevel", "must be empty, equipment has no fuel tank");
        } else if (tank != null && fuel == null) {
            errors.put("fuelLevel", "must not be null");
        } else if (tank != null && fuel.compareTo(tank) > 0) {
            errors.put("fuelLevel", "must not exceed tank capacity (%s l)".formatted(tank));
        }
        if (request.mileageOrHours() < unit.getMileageOrHours()) {
            errors.put("mileageOrHours",
                    "must not be less than current unit reading (%d)".formatted(unit.getMileageOrHours()));
        }
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(errors);
        }
    }

    // @Digits pilnuje 2 miejsc, tu tylko ujednolicamy zapis (8.5 → 8.50)
    private static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }
}
