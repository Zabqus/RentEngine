package com.wzabek.rentengine.rental.service;

import com.wzabek.rentengine.catalog.entity.Category;
import com.wzabek.rentengine.catalog.entity.CategoryType;
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
import com.wzabek.rentengine.user.entity.Role;
import com.wzabek.rentengine.user.entity.User;
import com.wzabek.rentengine.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2030, 6, 1, 10, 0);

    @Mock
    private RentalAgreementRepository agreementRepository;
    @Mock
    private EquipmentUnitRepository unitRepository;
    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private UserRepository userRepository;

    private RentalService service;
    private User client;
    private EquipmentUnit unit;

    @BeforeEach
    void setUp() {
        service = new RentalService(agreementRepository, unitRepository, settlementRepository, userRepository,
                new PricingCalculator(), new RentalMapper());

        client = new User("jan@test.pl", "hash", "Jan", "Kowalski", Role.CLIENT);
        Category category = new Category("Minikoparki", CategoryType.EXCAVATOR, List.of());
        Equipment equipment = new Equipment("Kubota U17-3", category, new BigDecimal("450.00"),
                new BigDecimal("3000.00"), null, Map.of());
        unit = new EquipmentUnit(equipment, "KUB-1", 100);
    }

    @Test
    void reserveCalculatesBasePriceAndCopiesDeposit() {
        givenClientAndUnit();
        when(agreementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RentalResponse response = service.reserve(request(START, START.plusHours(49)));

        ArgumentCaptor<RentalAgreement> saved = ArgumentCaptor.forClass(RentalAgreement.class);
        verify(agreementRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(AgreementStatus.RESERVED);
        assertThat(response.basePrice()).isEqualByComparingTo("1350.00"); // 49 h = 3 doby × 450
        assertThat(response.deposit()).isEqualByComparingTo("3000.00");
    }

    @Test
    void overlappingReservationIsRejected() {
        givenClientAndUnit();
        when(agreementRepository.existsOverlapping(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.reserve(request(START, START.plusDays(2))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("UNIT_NOT_AVAILABLE");
        verify(agreementRepository, never()).save(any());
    }

    @Test
    void unitInServiceIsRejected() {
        givenClientAndUnit();
        unit.setStatus(UnitStatus.IN_SERVICE);

        assertThatThrownBy(() -> service.reserve(request(START, START.plusDays(2))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("UNIT_IN_SERVICE");
        verify(agreementRepository, never()).save(any());
    }

    @Test
    void clientWithUnpaidSettlementIsRejected() {
        givenClientAndUnit();
        when(settlementRepository.existsByAgreementClientIdAndPaidFalse(any())).thenReturn(true);

        assertThatThrownBy(() -> service.reserve(request(START, START.plusDays(2))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("UNPAID_SETTLEMENT");
        verify(agreementRepository, never()).save(any());
    }

    @Test
    void endBeforeStartIsRejectedBeforeAnyLookup() {
        assertThatThrownBy(() -> service.reserve(request(START, START.minusHours(1))))
                .isInstanceOf(InvalidRequestException.class);
        verify(unitRepository, never()).findByIdForReservation(any());
    }

    @Test
    void missingUnitGives404() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(unitRepository.findByIdForReservation(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reserve(request(START, START.plusDays(1))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void reservedAgreementCanBeCancelled() {
        RentalAgreement agreement = agreement();
        when(agreementRepository.findById(10L)).thenReturn(Optional.of(agreement));

        RentalResponse response = service.cancel(10L);

        assertThat(response.status()).isEqualTo(AgreementStatus.CANCELLED);
    }

    @Test
    void activeAgreementCannotBeCancelled() {
        RentalAgreement agreement = agreement();
        agreement.changeStatus(AgreementStatus.ACTIVE);
        when(agreementRepository.findById(10L)).thenReturn(Optional.of(agreement));

        assertThatThrownBy(() -> service.cancel(10L))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_STATUS_TRANSITION");
        assertThat(agreement.getStatus()).isEqualTo(AgreementStatus.ACTIVE);
    }

    private RentalAgreement agreement() {
        return new RentalAgreement(client, unit, START, START.plusDays(2),
                new BigDecimal("900.00"), new BigDecimal("3000.00"));
    }

    private void givenClientAndUnit() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(client));
        when(unitRepository.findByIdForReservation(2L)).thenReturn(Optional.of(unit));
    }

    private static RentalRequest request(LocalDateTime start, LocalDateTime end) {
        return new RentalRequest(1L, 2L, start, end);
    }
}
