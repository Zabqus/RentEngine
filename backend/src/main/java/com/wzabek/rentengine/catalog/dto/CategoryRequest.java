package com.wzabek.rentengine.catalog.dto;

import com.wzabek.rentengine.catalog.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull CategoryType type,
        @NotNull List<@NotBlank @Size(max = 50) String> requiredParams
) {
}
