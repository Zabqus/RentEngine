package com.wzabek.rentengine.catalog.dto;

import com.wzabek.rentengine.catalog.entity.CategoryType;

import java.math.BigDecimal;
import java.util.Map;

public record EquipmentResponse(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        CategoryType categoryType,
        BigDecimal dailyPrice,
        BigDecimal deposit,
        BigDecimal tankCapacity,
        Map<String, Object> params
) {
}
