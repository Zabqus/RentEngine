package com.wzabek.rentengine.catalog.dto;

import com.wzabek.rentengine.catalog.entity.UnitStatus;
import jakarta.validation.constraints.NotNull;

public record UnitStatusRequest(@NotNull UnitStatus status) {
}
