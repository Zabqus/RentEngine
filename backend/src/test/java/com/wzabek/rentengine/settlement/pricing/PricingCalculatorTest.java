package com.wzabek.rentengine.settlement.pricing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingCalculatorTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 6, 1, 10, 0);

    private final PricingCalculator calculator = new PricingCalculator();

    @ParameterizedTest(name = "{0} min -> {1} day(s)")
    @CsvSource({
            "1, 1",
            "60, 1",
            "1440, 1",
            "1441, 2",
            "2880, 2",
            "4321, 4"
    })
    void everyStartedDayCountsAsFullDay(long minutes, long expectedDays) {
        assertThat(calculator.rentalDays(START, START.plusMinutes(minutes))).isEqualTo(expectedDays);
    }

    @Test
    void basePriceIsDaysTimesDailyPrice() {
        BigDecimal price = calculator.basePrice(new BigDecimal("189.00"), START, START.plusDays(3));

        assertThat(price).isEqualByComparingTo("567.00");
        assertThat(price.scale()).isEqualTo(2);
    }

    @Test
    void oneHourOverDayIsChargedAsTwoDays() {
        BigDecimal price = calculator.basePrice(new BigDecimal("450.00"), START, START.plusHours(25));

        assertThat(price).isEqualByComparingTo("900.00");
    }

    @Test
    void endNotAfterStartIsRejected() {
        assertThatThrownBy(() -> calculator.rentalDays(START, START))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.rentalDays(START, START.minusHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
