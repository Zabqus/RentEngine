package com.wzabek.rentengine.common.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Object id) {
        super("%s with id %s not found".formatted(resource, id));
    }
}
