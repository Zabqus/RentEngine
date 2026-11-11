package com.wzabek.rentengine.settlement.service;

import com.wzabek.rentengine.catalog.entity.Category;
import com.wzabek.rentengine.catalog.entity.CategoryType;
import com.wzabek.rentengine.catalog.entity.Equipment;
import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.common.exception.ForbiddenOperationException;
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
import com.wzabek.rentengine.settlement.event.SettlementCreatedEvent;
import com.wzabek.rentengine.settlement.mapper.SettlementMapper;
import com.wzabek.rentengine.settlement.pricing.PricingCalculator;
import com.wzabek.rentengine.settlement.repository.SettlementRepository;
import com.wzabek.rentengine.user.entity.Role;
import com.wzabek.rentengine.user.entity.User;
import com.wzabek.rentengine.user.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDateTime START = LocalDateTime.of(2030, 6, 1, 10, 0);
    private static final LocalDateTime END = START.plusDays(2);
    private static final LocalDateTime NOW = END.plusDays(1);
    private static final Long AGREEMENT_ID = 10L;

    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private ProtocolRepository protocolRepository;
    @Mock
    private DamageRepository damageRepository;
    @Mock
    private RentalService rentalService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SettlementService service;
    private RentalAgreement agreement;
    private User employee;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        service = new SettlementService(settlementRepository, protocolRepository, damageRepository, rentalService,
                new PricingCalculator(), new PricingProperties(new BigDecimal("7.00")), new SettlementMapper(),
                eventPublisher, clock);

        Category category = new Category("Minikoparki", CategoryType.EXCAVATOR, List.of());
        Equipment equipment = new Equipment("Kubota U17-3", category, new BigDecimal("450.00"),
                new BigDecimal("3000.00"), new BigDecimal("19.00"), Map.of());
        EquipmentUnit unit = new EquipmentUnit(equipment, "KUB-1", 100);
        User client = new User("jan@test.pl", "hash", "Jan", "Kowalski", Role.CLIENT);
        agreement = new RentalAgreement(client, unit, START, END, new BigDecimal("900.00"), new BigDecimal("3000.00"));
        ReflectionTestUtils.setField(agreement, "id", AGREEMENT_ID);
        employee = new User("emp@test.pl", "hash", "Piotr", "Pracownik", Role.EMPLOYEE);
    }

    private void givenReturnedAgreement(LocalDateTime returnedAt) {
        agreement.changeStatus(AgreementStatus.ACTIVE);
        agreement.changeStatus(AgreementStatus.RETURNED);
        Protocol issue = new Protocol(agreement, ProtocolType.ISSUE, new BigDecimal("19.00"), 100, null, employee);
        Protocol returned = new Protocol(agreement, ProtocolType.RETURN, new BigDecimal("9.00"), 150, null, employee);
        ReflectionTestUtils.setField(returned, "createdAt", returnedAt);
        // lenient: pay() potrzebuje tylko protokołu zwrotu
        lenient().when(rentalService.getAgreement(AGREEMENT_ID)).thenReturn(agreement);
        lenient().when(protocolRepository.findByAgreementIdAndType(AGREEMENT_ID, ProtocolType.ISSUE)).thenReturn(Optional.of(issue));
        when(protocolRepository.findByAgreementIdAndType(AGREEMENT_ID, ProtocolType.RETURN)).thenReturn(Optional.of(returned));
    }

    @Test
    void createChargesLateFuelAndReturnDamages() {
        givenReturnedAgreement(END.plusHours(30));
        when(damageRepository.sumCost(AGREEMENT_ID, ProtocolType.RETURN)).thenReturn(new BigDecimal("3200.00"));
        when(settlementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SettlementResponse response = service.create(AGREEMENT_ID);

        // stawka 450: 2 doby spóźnienia = 90; 10 l × 7 = 70; szkody 3200 - kaucja 3000 = 200
        assertThat(response.lateDays()).isEqualTo(2);
        assertThat(response.lateFee()).isEqualByComparingTo("90.00");
        assertThat(response.fuelFee()).isEqualByComparingTo("70.00");
        assertThat(response.depositDeduction()).isEqualByComparingTo("3000.00");
        assertThat(response.damageFee()).isEqualByComparingTo("200.00");
        assertThat(response.depositRefund()).isEqualByComparingTo("0.00");
        assertThat(response.total()).isEqualByComparingTo("1260.00");
        assertThat(response.paid()).isFalse();
        verify(eventPublisher).publishEvent(any(SettlementCreatedEvent.class));
    }

    @Test
    void createForNotReturnedAgreementIsRejected() {
        when(rentalService.getAgreement(AGREEMENT_ID)).thenReturn(agreement);

        assertThatThrownBy(() -> service.create(AGREEMENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("AGREEMENT_NOT_RETURNED");
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void secondSettlementIsRejected() {
        when(rentalService.getAgreement(AGREEMENT_ID)).thenReturn(agreement);
        when(settlementRepository.existsByAgreementId(AGREEMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.create(AGREEMENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("SETTLEMENT_EXISTS");
    }

    private Settlement givenSettlement() {
        Settlement settlement = new Settlement(agreement, new BigDecimal("900.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("500.00"), new BigDecimal("900.00"));
        ReflectionTestUtils.setField(settlement, "id", 5L);
        when(settlementRepository.findById(5L)).thenReturn(Optional.of(settlement));
        return settlement;
    }

    @Test
    void payMarksSettlementPaidAndClosesAgreement() {
        givenReturnedAgreement(END);
        Settlement settlement = givenSettlement();

        SettlementResponse response = service.pay(5L);

        assertThat(settlement.isPaid()).isTrue();
        assertThat(settlement.getPaidAt()).isEqualTo(NOW);
        assertThat(agreement.getStatus()).isEqualTo(AgreementStatus.SETTLED);
        assertThat(response.depositRefund()).isEqualByComparingTo("2500.00");
    }

    @Test
    void payingTwiceIsRejected() {
        Settlement settlement = givenSettlement();
        settlement.markPaid(NOW);

        assertThatThrownBy(() -> service.pay(5L))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("SETTLEMENT_ALREADY_PAID");
    }

    @Test
    void clientCannotSeeForeignSettlement() {
        AuthenticatedUser stranger = new AuthenticatedUser(99L, "x@test.pl", Role.CLIENT);
        when(rentalService.getAccessibleAgreement(AGREEMENT_ID, stranger))
                .thenThrow(new ForbiddenOperationException("nope"));

        assertThatThrownBy(() -> service.findByAgreement(AGREEMENT_ID, stranger))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(settlementRepository, never()).findByAgreementId(any());
    }
}
