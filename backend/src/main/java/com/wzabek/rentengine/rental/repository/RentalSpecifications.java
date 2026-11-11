package com.wzabek.rentengine.rental.repository;

import com.wzabek.rentengine.rental.dto.RentalFilter;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import org.springframework.data.jpa.domain.Specification;

public final class RentalSpecifications {

    private RentalSpecifications() {
    }

    public static Specification<RentalAgreement> matching(RentalFilter filter) {
        return Specification.allOf(
                equalTo("status", filter.status()),
                equalTo("client", filter.clientId()),
                equalTo("unit", filter.unitId())
        );
    }

    // Dla relacji porównujemy id (client.id, unit.id), dla zwykłych pól samą wartość
    private static Specification<RentalAgreement> equalTo(String attribute, Object value) {
        if (value == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> value instanceof Long id
                ? cb.equal(root.get(attribute).get("id"), id)
                : cb.equal(root.get(attribute), value);
    }
}
