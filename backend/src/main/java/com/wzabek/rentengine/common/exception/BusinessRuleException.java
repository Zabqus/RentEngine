package com.wzabek.rentengine.common.exception;

import lombok.Getter;

// Złamana reguła biznesowa (R1–R8, kolizje, blokady usuwania) → 409
@Getter
public class BusinessRuleException extends RuntimeException {

    // Stały kod błędu, po którym frontend może rozpoznać przypadek, np. UNIT_IN_SERVICE
    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }
}
