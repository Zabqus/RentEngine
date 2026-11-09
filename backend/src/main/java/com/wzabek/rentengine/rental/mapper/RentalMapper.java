package com.wzabek.rentengine.rental.mapper;

import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import com.wzabek.rentengine.rental.dto.RentalResponse;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import com.wzabek.rentengine.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class RentalMapper {

    public RentalResponse toResponse(RentalAgreement agreement) {
        User client = agreement.getClient();
        EquipmentUnit unit = agreement.getUnit();
        return new RentalResponse(
                agreement.getId(),
                client.getId(),
                client.getFirstName() + " " + client.getLastName(),
                unit.getId(),
                unit.getSerialOrPlate(),
                unit.getEquipment().getId(),
                unit.getEquipment().getName(),
                agreement.getStartDate(),
                agreement.getEndDate(),
                agreement.getStatus(),
                agreement.getBasePrice(),
                agreement.getDeposit(),
                agreement.getCreatedAt()
        );
    }
}
