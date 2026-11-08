package com.wzabek.rentengine.rental.entity;

import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import com.wzabek.rentengine.common.exception.BusinessRuleException;
import com.wzabek.rentengine.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_agreements")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RentalAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private EquipmentUnit unit;

    // Data z godziną, bo kara (R3) liczy każdą rozpoczętą dobę spóźnienia
    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    // Bez settera: status zmieniamy tylko przez changeStatus(), żeby pilnować R8
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private AgreementStatus status = AgreementStatus.RESERVED;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    // Kopia kaucji z chwili rezerwacji, żeby zmiana cennika nie ruszała umowy
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deposit;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    public RentalAgreement(User client, EquipmentUnit unit, LocalDateTime startDate, LocalDateTime endDate,
                           BigDecimal basePrice, BigDecimal deposit) {
        this.client = client;
        this.unit = unit;
        this.startDate = startDate;
        this.endDate = endDate;
        this.basePrice = basePrice;
        this.deposit = deposit;
    }

    public void changeStatus(AgreementStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessRuleException("INVALID_STATUS_TRANSITION",
                    "Cannot change agreement status from %s to %s".formatted(status, target));
        }
        status = target;
    }
}
