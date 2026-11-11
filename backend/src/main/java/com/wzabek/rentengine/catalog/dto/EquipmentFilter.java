package com.wzabek.rentengine.catalog.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Parametry GET /api/equipment; wszystkie opcjonalne. from + to = tylko sprzęt z wolnym egzemplarzem w tym terminie
public record EquipmentFilter(
        @Positive Long categoryId,
        @Size(max = 100) String q,
        @PositiveOrZero BigDecimal minPrice,
        @PositiveOrZero BigDecimal maxPrice,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
) {
}
