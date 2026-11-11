package com.wzabek.rentengine.protocol.dto;

import com.wzabek.rentengine.protocol.entity.DamageSeverity;

import java.math.BigDecimal;

public record DamageResponse(
        Long id,
        Long protocolId,
        String description,
        DamageSeverity severity,
        BigDecimal cost,
        // null, gdy brak zdjęcia
        String photoUrl
) {
}
