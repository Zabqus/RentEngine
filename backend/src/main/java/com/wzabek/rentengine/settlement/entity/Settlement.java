package com.wzabek.rentengine.settlement.entity;

import com.wzabek.rentengine.rental.entity.RentalAgreement;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id", nullable = false, unique = true)
    private RentalAgreement agreement;

    @Column(name = "base_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "late_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal lateFee;

    @Column(name = "fuel_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal fuelFee;

    @Column(name = "damage_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal damageFee;

    // Część kosztu szkód pokryta z kaucji (R5)
    @Column(name = "deposit_deduction", nullable = false, precision = 10, scale = 2)
    private BigDecimal depositDeduction;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(nullable = false)
    private boolean paid;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public Settlement(RentalAgreement agreement, BigDecimal baseAmount, BigDecimal lateFee, BigDecimal fuelFee,
                      BigDecimal damageFee, BigDecimal depositDeduction, BigDecimal total) {
        this.agreement = agreement;
        this.baseAmount = baseAmount;
        this.lateFee = lateFee;
        this.fuelFee = fuelFee;
        this.damageFee = damageFee;
        this.depositDeduction = depositDeduction;
        this.total = total;
    }
}
