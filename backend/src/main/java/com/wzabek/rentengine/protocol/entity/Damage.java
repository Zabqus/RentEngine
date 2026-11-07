package com.wzabek.rentengine.protocol.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "damages")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Damage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "protocol_id", nullable = false)
    private Protocol protocol;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DamageSeverity severity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "photo_path")
    private String photoPath;

    public Damage(String description, DamageSeverity severity, BigDecimal cost) {
        this.description = description;
        this.severity = severity;
        this.cost = cost;
    }
}
