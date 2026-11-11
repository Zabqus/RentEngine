package com.wzabek.rentengine.protocol.dto;

import com.wzabek.rentengine.protocol.entity.ProtocolType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

// fuelLevel w litrach; wymagane tylko dla sprzętu ze zbiornikiem (sprawdza serwis)
public record ProtocolRequest(
        @NotNull ProtocolType type,
        @PositiveOrZero @Digits(integer = 4, fraction = 2) BigDecimal fuelLevel,
        @NotNull @PositiveOrZero Integer mileageOrHours,
        @Size(max = 2000) String notes
) {
}
