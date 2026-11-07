package com.wzabek.rentengine.catalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "equipment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "daily_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyPrice;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal deposit;

    // Litry; null dla sprzętu bez zbiornika
    @Column(name = "tank_capacity", precision = 6, scale = 2)
    private BigDecimal tankCapacity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> params = new HashMap<>();

    public Equipment(String name, Category category, BigDecimal dailyPrice, BigDecimal deposit,
                     BigDecimal tankCapacity, Map<String, Object> params) {
        this.name = name;
        this.category = category;
        this.dailyPrice = dailyPrice;
        this.deposit = deposit;
        this.tankCapacity = tankCapacity;
        this.params = params;
    }
}
