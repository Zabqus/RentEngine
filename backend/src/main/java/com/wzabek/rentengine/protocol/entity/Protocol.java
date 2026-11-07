package com.wzabek.rentengine.protocol.entity;

import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "protocols")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Protocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id", nullable = false)
    private RentalAgreement agreement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProtocolType type;

    // Litry w zbiorniku; null dla sprzętu bez zbiornika
    @Column(name = "fuel_level", precision = 6, scale = 2)
    private BigDecimal fuelLevel;

    @Column(name = "mileage_or_hours", nullable = false)
    private int mileageOrHours;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "protocol", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Damage> damages = new ArrayList<>();

    public Protocol(RentalAgreement agreement, ProtocolType type, BigDecimal fuelLevel,
                    int mileageOrHours, String notes, User employee) {
        this.agreement = agreement;
        this.type = type;
        this.fuelLevel = fuelLevel;
        this.mileageOrHours = mileageOrHours;
        this.notes = notes;
        this.employee = employee;
    }

    public void addDamage(Damage damage) {
        damages.add(damage);
        damage.setProtocol(this);
    }
}
