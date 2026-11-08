package com.wzabek.rentengine.common.exception;

// Użytkownik jest zalogowany, ale zasób nie należy do niego (np. cudza umowa) → 403
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
