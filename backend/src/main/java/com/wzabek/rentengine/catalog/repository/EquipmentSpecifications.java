package com.wzabek.rentengine.catalog.repository;

import com.wzabek.rentengine.catalog.dto.EquipmentFilter;
import com.wzabek.rentengine.catalog.entity.Equipment;
import com.wzabek.rentengine.catalog.entity.EquipmentUnit;
import com.wzabek.rentengine.catalog.entity.UnitStatus;
import com.wzabek.rentengine.rental.entity.AgreementStatus;
import com.wzabek.rentengine.rental.entity.RentalAgreement;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

// Filtry katalogu składane dynamicznie; pusty parametr = unrestricted(), czyli brak warunku
public final class EquipmentSpecifications {

    private EquipmentSpecifications() {
    }

    public static Specification<Equipment> matching(EquipmentFilter filter) {
        return Specification.allOf(
                inCategory(filter.categoryId()),
                nameContains(filter.q()),
                priceAtLeast(filter.minPrice()),
                priceAtMost(filter.maxPrice()),
                availableBetween(filter.from(), filter.to())
        );
    }

    static Specification<Equipment> inCategory(Long categoryId) {
        return categoryId == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    static Specification<Equipment> nameContains(String q) {
        if (q == null || q.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + escapeLike(q.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\');
    }

    static Specification<Equipment> priceAtLeast(BigDecimal min) {
        return min == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dailyPrice"), min);
    }

    static Specification<Equipment> priceAtMost(BigDecimal max) {
        return max == null ? Specification.unrestricted()
                : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dailyPrice"), max);
    }

    // Istnieje egzemplarz AVAILABLE bez kolidującej umowy; ta sama reguła co R1 w RentalAgreementRepository.existsOverlapping
    static Specification<Equipment> availableBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            Subquery<Long> units = query.subquery(Long.class);
            Root<EquipmentUnit> unit = units.from(EquipmentUnit.class);

            Subquery<Long> conflicts = units.subquery(Long.class);
            Root<RentalAgreement> agreement = conflicts.from(RentalAgreement.class);
            conflicts.select(agreement.get("id")).where(
                    cb.equal(agreement.get("unit"), unit),
                    cb.lessThan(agreement.get("startDate"), to),
                    cb.or(
                            cb.and(agreement.get("status").in(AgreementStatus.RESERVED, AgreementStatus.ACTIVE),
                                    cb.greaterThan(agreement.get("endDate"), from)),
                            cb.equal(agreement.get("status"), AgreementStatus.OVERDUE)
                    ));

            units.select(unit.get("id")).where(
                    cb.equal(unit.get("equipment"), root),
                    cb.equal(unit.get("status"), UnitStatus.AVAILABLE),
                    cb.not(cb.exists(conflicts)));
            return cb.exists(units);
        };
    }

    // % i _ wpisane przez użytkownika mają być zwykłymi znakami, nie wzorcem
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
