package com.resturant.management.rms.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base for exceptions that map directly onto an HTTP status. */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
