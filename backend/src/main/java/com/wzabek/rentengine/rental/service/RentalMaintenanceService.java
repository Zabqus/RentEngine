package com.wzabek.rentengine.rental.service;

import com.wzabek.rentengine.rental.config.RentalProperties;
import com.wzabek.rentengine.rental.entity.AgreementStatus;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.rental.event.AgreementOverdueEvent;
import com.wzabek.rentengine.rental.event.ReservationExpiredEvent;
import com.wzabek.rentengine.rental.repository.RentalAgreementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

// Zmiany statusów wykonywane przez scheduler (ACTIVE → OVERDUE, RESERVED → CANCELLED).
// Każda umowa w osobnej transakcji: konflikt na jednej (np. pracownik właśnie ją wydaje) nie cofa reszty.
@Slf4j
@Service
@RequiredArgsConstructor
public class RentalMaintenanceService {

    private final RentalAgreementRepository agreementRepository;
    private final RentalProperties rentalProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public int markOverdue() {
        LocalDateTime now = LocalDateTime.now(clock);
        return process(agreementRepository.findOverdueIds(now),
                a -> a.getStatus() == AgreementStatus.ACTIVE && a.getEndDate().isBefore(now),
                AgreementStatus.OVERDUE,
                a -> eventPublisher.publishEvent(new AgreementOverdueEvent(a.getId())));
    }

    public int cancelUnclaimedReservations() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime pickupDeadline = now.minus(rentalProperties.pickupGrace());
        return process(agreementRepository.findUnclaimedIds(pickupDeadline, now),
                a -> a.getStatus() == AgreementStatus.RESERVED
                        && (a.getStartDate().isBefore(pickupDeadline) || !a.getEndDate().isAfter(now)),
                AgreementStatus.CANCELLED,
                a -> eventPublisher.publishEvent(new ReservationExpiredEvent(a.getId())));
    }

    private int process(List<Long> ids, Predicate<RentalAgreement> stillApplies, AgreementStatus target,
                        Consumer<RentalAgreement> afterChange) {
        int changed = 0;
        for (Long id : ids) {
            try {
                Boolean done = transactionTemplate.execute(status -> {
                    // Stan mógł się zmienić od zapytania o listę, więc sprawdzamy ponownie
                    RentalAgreement agreement = agreementRepository.findById(id).orElse(null);
                    if (agreement == null || !stillApplies.test(agreement)) {
                        return false;
                    }
                    agreement.changeStatus(target);
                    afterChange.accept(agreement);
                    return true;
                });
                if (Boolean.TRUE.equals(done)) {
                    changed++;
                }
            } catch (RuntimeException e) {
                log.warn("Cannot change agreement {} to {}: {}", id, target, e.getMessage());
            }
        }
        if (changed > 0) {
            log.info("Scheduler changed {} agreement(s) to {}", changed, target);
        }
        return changed;
    }
}
