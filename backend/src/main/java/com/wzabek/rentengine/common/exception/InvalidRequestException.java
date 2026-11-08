package com.wzabek.rentengine.common.exception;

import lombok.Getter;

import java.util.Map;

// Błąd walidacji, który da się wykryć dopiero w serwisie (np. params zależne od kategorii) → 400
@Getter
public class InvalidRequestException extends RuntimeException {

    private final Map<String, String> errors;

    public InvalidRequestException(Map<String, String> errors) {
        super("Request contains invalid data");
        this.errors = Map.copyOf(errors);
    }
}
