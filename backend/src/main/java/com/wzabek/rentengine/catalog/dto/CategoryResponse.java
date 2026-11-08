package com.wzabek.rentengine.catalog.dto;

import com.wzabek.rentengine.catalog.entity.CategoryType;

import java.util.List;

public record CategoryResponse(
        Long id,
        String name,
        CategoryType type,
        List<String> requiredParams
) {
}
