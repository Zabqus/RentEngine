package com.wzabek.rentengine.protocol.mapper;

import com.wzabek.rentengine.protocol.dto.DamageResponse;
import com.wzabek.rentengine.protocol.dto.ProtocolResponse;
import com.wzabek.rentengine.protocol.entity.Damage;
import com.wzabek.rentengine.protocol.entity.Protocol;
import com.wzabek.rentengine.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ProtocolMapper {

    public ProtocolResponse toResponse(Protocol protocol) {
        User employee = protocol.getEmployee();
        return new ProtocolResponse(
                protocol.getId(),
                protocol.getAgreement().getId(),
                protocol.getType(),
                protocol.getFuelLevel(),
                protocol.getMileageOrHours(),
                protocol.getNotes(),
                employee.getId(),
                employee.getFirstName() + " " + employee.getLastName(),
                protocol.getCreatedAt(),
                protocol.getDamages().stream().map(this::toResponse).toList()
        );
    }

    public DamageResponse toResponse(Damage damage) {
        return new DamageResponse(
                damage.getId(),
                damage.getProtocol().getId(),
                damage.getDescription(),
                damage.getSeverity(),
                damage.getCost()
        );
    }
}
