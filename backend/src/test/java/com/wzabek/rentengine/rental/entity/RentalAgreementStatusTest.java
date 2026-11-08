package com.wzabek.rentengine.rental.entity;

import com.wzabek.rentengine.common.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RentalAgreementStatusTest {

    @Test
    void newAgreementIsReserved() {
        assertThat(newAgreement().getStatus()).isEqualTo(AgreementStatus.RESERVED);
    }

    @Test
    void fullHappyPathReachesSettled() {
        RentalAgreement agreement = newAgreement();

        agreement.changeStatus(AgreementStatus.ACTIVE);
        agreement.changeStatus(AgreementStatus.OVERDUE);
        agreement.changeStatus(AgreementStatus.RETURNED);
        agreement.changeStatus(AgreementStatus.SETTLED);

        assertThat(agreement.getStatus()).isEqualTo(AgreementStatus.SETTLED);
    }

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "RESERVED, ACTIVE",
            "RESERVED, CANCELLED",
            "ACTIVE, OVERDUE",
            "ACTIVE, RETURNED",
            "OVERDUE, RETURNED",
            "RETURNED, SETTLED"
    })
    void allowedTransitions(AgreementStatus from, AgreementStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "RESERVED, RETURNED",
            "RESERVED, SETTLED",
            "ACTIVE, CANCELLED",
            "ACTIVE, RESERVED",
            "OVERDUE, ACTIVE",
            "RETURNED, ACTIVE",
            "SETTLED, RESERVED",
            "CANCELLED, ACTIVE",
            "RESERVED, RESERVED"
    })
    void forbiddenTransitions(AgreementStatus from, AgreementStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @Test
    void invalidTransitionThrowsAndKeepsStatus() {
        RentalAgreement agreement = newAgreement();

        assertThatThrownBy(() -> agreement.changeStatus(AgreementStatus.SETTLED))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("INVALID_STATUS_TRANSITION");
        assertThat(agreement.getStatus()).isEqualTo(AgreementStatus.RESERVED);
    }

    @Test
    void cancelledAgreementCannotBeActivated() {
        RentalAgreement agreement = newAgreement();
        agreement.changeStatus(AgreementStatus.CANCELLED);

        assertThatThrownBy(() -> agreement.changeStatus(AgreementStatus.ACTIVE))
                .isInstanceOf(BusinessRuleException.class);
    }

    private static RentalAgreement newAgreement() {
        LocalDateTime start = LocalDateTime.of(2026, 6, 1, 10, 0);
        return new RentalAgreement(null, null, start, start.plusDays(2), new BigDecimal("100.00"), BigDecimal.ZERO);
    }
}
