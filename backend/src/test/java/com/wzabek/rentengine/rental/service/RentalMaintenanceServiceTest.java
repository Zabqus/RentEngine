package com.wzabek.rentengine.rental.service;

import com.wzabek.rentengine.catalog.entity.Category;
import com.wzabek.rentengine.catalog.entity.CategoryType;
import com.wzabek.rentengine.catalog.entity.Equipment;
import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import com.wzabek.rentengine.rental.config.RentalProperties;
import com.wzabek.rentengine.rental.entity.AgreementStatus;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.rental.event.AgreementOverdueEvent;
import com.wzabek.rentengine.rental.event.ReservationExpiredEvent;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import com.wzabek.rentengine.user.entity.Role;
import com.wzabek.rentengine.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalMaintenanceServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDateTime NOW = LocalDateTime.of(2030, 6, 10, 12, 0);

    @Mock
    private RentalAgreementRepository agreementRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RentalMaintenanceService service;
    private EquipmentUnit unit;
    private User client;

    @BeforeEach
    void setUp() {
        // Mock menedżera transakcji: TransactionTemplate po prostu wykonuje callback
        TransactionTemplate tx = new TransactionTemplate(mock(PlatformTransactionManager.class));
        service = new RentalMaintenanceService(agreementRepository, new RentalProperties(Duration.ofHours(24)),
                eventPublisher, tx, Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));

        Category category = new Category("Minikoparki", CategoryType.EXCAVATOR, List.of());
        Equipment equipment = new Equipment("Kubota", category, new BigDecimal("450.00"),
                new BigDecimal("3000.00"), null, Map.of());
        unit = new EquipmentUnit(equipment, "KUB-1", 100);
        client = new User("jan@test.pl", "hash", "Jan", "Kowalski", Role.CLIENT);
    }

    private RentalAgreement agreement(long id, LocalDateTime start, LocalDateTime end, AgreementStatus... path) {
        RentalAgreement agreement = new RentalAgreement(client, unit, start, end,
                new BigDecimal("900.00"), new BigDecimal("3000.00"));
        ReflectionTestUtils.setField(agreement, "id", id);
        for (AgreementStatus status : path) {
            agreement.changeStatus(status);
        }
        when(agreementRepository.findById(id)).thenReturn(Optional.of(agreement));
        return agreement;
    }

    @Test
    void activeAgreementPastEndDateBecomesOverdue() {
        RentalAgreement late = agreement(1L, NOW.minusDays(3), NOW.minusMinutes(1), AgreementStatus.ACTIVE);
        when(agreementRepository.findOverdueIds(NOW)).thenReturn(List.of(1L));

        assertThat(service.markOverdue()).isEqualTo(1);

        assertThat(late.getStatus()).isEqualTo(AgreementStatus.OVERDUE);
        verify(eventPublisher).publishEvent(new AgreementOverdueEvent(1L));
    }

    @Test
    void agreementReturnedInTheMeantimeIsSkipped() {
        RentalAgreement returned = agreement(1L, NOW.minusDays(3), NOW.minusMinutes(1),
                AgreementStatus.ACTIVE, AgreementStatus.RETURNED);
        when(agreementRepository.findOverdueIds(NOW)).thenReturn(List.of(1L));

        assertThat(service.markOverdue()).isZero();

        assertThat(returned.getStatus()).isEqualTo(AgreementStatus.RETURNED);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void reservationNotPickedUpWithinGraceIsCancelled() {
        RentalAgreement unclaimed = agreement(2L, NOW.minusHours(25), NOW.plusDays(2));
        when(agreementRepository.findUnclaimedIds(NOW.minusHours(24), NOW)).thenReturn(List.of(2L));

        assertThat(service.cancelUnclaimedReservations()).isEqualTo(1);

        assertThat(unclaimed.getStatus()).isEqualTo(AgreementStatus.CANCELLED);
        verify(eventPublisher).publishEvent(new ReservationExpiredEvent(2L));
    }

    @Test
    void reservationStillWithinGraceIsKept() {
        RentalAgreement recent = agreement(2L, NOW.minusHours(23), NOW.plusDays(2));
        when(agreementRepository.findUnclaimedIds(NOW.minusHours(24), NOW)).thenReturn(List.of(2L));

        assertThat(service.cancelUnclaimedReservations()).isZero();
        assertThat(recent.getStatus()).isEqualTo(AgreementStatus.RESERVED);
    }

    @Test
    void reservationWhosePeriodAlreadyEndedIsCancelledEvenWithinGrace() {
        RentalAgreement shortOne = agreement(2L, NOW.minusHours(5), NOW.minusHours(1));
        when(agreementRepository.findUnclaimedIds(NOW.minusHours(24), NOW)).thenReturn(List.of(2L));

        assertThat(service.cancelUnclaimedReservations()).isEqualTo(1);
        assertThat(shortOne.getStatus()).isEqualTo(AgreementStatus.CANCELLED);
    }

    @Test
    void failureOnOneAgreementDoesNotStopOthers() {
        when(agreementRepository.findOverdueIds(NOW)).thenReturn(List.of(1L, 3L));
        when(agreementRepository.findById(1L)).thenThrow(new ObjectOptimisticLockingFailureException(RentalAgreement.class, 1L));
        RentalAgreement other = agreement(3L, NOW.minusDays(3), NOW.minusHours(2), AgreementStatus.ACTIVE);

        assertThat(service.markOverdue()).isEqualTo(1);
        assertThat(other.getStatus()).isEqualTo(AgreementStatus.OVERDUE);
    }
}
