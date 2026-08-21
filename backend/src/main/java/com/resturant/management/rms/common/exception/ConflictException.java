package com.resturant.management.rms.common.exception;

import org.springframework.http.HttpStatus;

/** Duplicate username, duplicate code, or any other uniqueness violation. */
public class ConflictException extends ApiException {

    public ConflictException(String messageKey, Object... args) {
        super(HttpStatus.CONFLICT, messageKey, args);
    }
}
