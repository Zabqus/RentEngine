package com.wzabek.rentengine.rental.entity;

import java.util.Set;

// Cykl życia umowy (R8), diagram w plan.md / README
public enum AgreementStatus {
    RESERVED, ACTIVE, OVERDUE, RETURNED, SETTLED, CANCELLED;

    public Set<AgreementStatus> allowedNext() {
        return switch (this) {
            case RESERVED -> Set.of(ACTIVE, CANCELLED);
            case ACTIVE -> Set.of(OVERDUE, RETURNED);
            case OVERDUE -> Set.of(RETURNED);
            case RETURNED -> Set.of(SETTLED);
            case SETTLED, CANCELLED -> Set.of();
        };
    }

    public boolean canTransitionTo(AgreementStatus target) {
        return allowedNext().contains(target);
    }
}
