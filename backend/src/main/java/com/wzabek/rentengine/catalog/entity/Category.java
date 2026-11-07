package com.wzabek.rentengine.catalog.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type;

    // Klucze, które musi mieć Equipment.params w tej kategorii
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_params", nullable = false)
    private List<String> requiredParams = new ArrayList<>();

    public Category(String name, CategoryType type, List<String> requiredParams) {
        this.name = name;
        this.type = type;
        this.requiredParams = requiredParams;
    }
}
