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

    private static final LocalDateTime END = START.plusDays(3);
    private static final BigDecimal RATE = new BigDecimal("450.00");
    private static final BigDecimal FUEL_PRICE = new BigDecimal("7.00");

    @Test
    void returnOnTimeHasNoLateFee() {
        assertThat(calculator.lateFee(RATE, END, END)).isEqualByComparingTo("0.00");
        assertThat(calculator.lateFee(RATE, END, END.minusHours(5))).isEqualByComparingTo("0.00");
    }

    @Test
    void oneHourLateIsChargedAsFullDay() {
        assertThat(calculator.lateDays(END, END.plusHours(1))).isEqualTo(1);
        assertThat(calculator.lateFee(RATE, END, END.plusHours(1))).isEqualByComparingTo("45.00");
    }

    @ParameterizedTest(name = "{0} min late -> {1} day(s), fee {2}")
    @CsvSource({
            "4320, 3, 135.00",
            "4321, 4, 180.00"
    })
    void everyStartedLateDayAddsTenPercent(long minutes, long days, String fee) {
        assertThat(calculator.lateDays(END, END.plusMinutes(minutes))).isEqualTo(days);
        assertThat(calculator.lateFee(RATE, END, END.plusMinutes(minutes))).isEqualByComparingTo(fee);
    }

    @Test
    void missingFuelIsCharged() {
        assertThat(calculator.fuelFee(new BigDecimal("19.00"), new BigDecimal("12.50"), FUEL_PRICE))
                .isEqualByComparingTo("45.50");
    }

    @Test
    void extraFuelIsNotRefundedAndNoTankMeansNoFee() {
        assertThat(calculator.fuelFee(new BigDecimal("10.00"), new BigDecimal("15.00"), FUEL_PRICE))
                .isEqualByComparingTo("0.00");
        assertThat(calculator.fuelFee(null, null, FUEL_PRICE)).isEqualByComparingTo("0.00");
    }

    @Test
    void damagesBelowDepositAreCoveredByDeposit() {
        SettlementAmounts amounts = calculator.settle(input("900.00", "3000.00", END, null, null, "1200.00"));

        assertThat(amounts.depositDeduction()).isEqualByComparingTo("1200.00");
        assertThat(amounts.damageFee()).isEqualByComparingTo("0.00");
        assertThat(amounts.total()).isEqualByComparingTo("900.00");
    }

    @Test
    void damagesAboveDepositAreBilled() {
        SettlementAmounts amounts = calculator.settle(input("900.00", "3000.00", END, null, null, "5000.00"));

        assertThat(amounts.depositDeduction()).isEqualByComparingTo("3000.00");
        assertThat(amounts.damageFee()).isEqualByComparingTo("2000.00");
        assertThat(amounts.total()).isEqualByComparingTo("2900.00");
    }

    @Test
    void settlementCombinesAllFeesWithRateTakenFromAgreement() {
        // 3 doby za 1350 → stawka 450; spóźnienie 2 h → 1 doba → 45; brak 6.5 l → 45.50; szkody 3500 przy kaucji 3000
        SettlementAmounts amounts = calculator.settle(
                input("1350.00", "3000.00", END.plusHours(2), "19.00", "12.50", "3500.00"));

        assertThat(amounts.baseAmount()).isEqualByComparingTo("1350.00");
        assertThat(amounts.lateDays()).isEqualTo(1);
        assertThat(amounts.lateFee()).isEqualByComparingTo("45.00");
        assertThat(amounts.fuelFee()).isEqualByComparingTo("45.50");
        assertThat(amounts.depositDeduction()).isEqualByComparingTo("3000.00");
        assertThat(amounts.damageFee()).isEqualByComparingTo("500.00");
        assertThat(amounts.total()).isEqualByComparingTo("1940.50");
    }

    private static SettlementInput input(String base, String deposit, LocalDateTime returnedAt,
                                         String issuedFuel, String returnedFuel, String damageCost) {
        return new SettlementInput(new BigDecimal(base), new BigDecimal(deposit), START, END, returnedAt,
                issuedFuel == null ? null : new BigDecimal(issuedFuel),
                returnedFuel == null ? null : new BigDecimal(returnedFuel),
                FUEL_PRICE, new BigDecimal(damageCost));
    }
}
