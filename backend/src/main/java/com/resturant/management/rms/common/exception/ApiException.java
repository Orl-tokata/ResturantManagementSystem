package com.resturant.management.rms.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base for exceptions that map directly onto an HTTP status.
 *
 * <p>Carries a <em>message key</em> and its arguments rather than a finished
 * sentence. The text is resolved in {@link GlobalExceptionHandler}, the one
 * place that knows the locale of the request being answered — a service has no
 * business picking a language, and by the time it throws, the response is
 * several layers away.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String messageKey;
    private final transient Object[] args;

    public ApiException(HttpStatus status, String messageKey, Object... args) {
        // The key becomes getMessage(), so a stack trace still identifies which
        // error this was. Nothing user-facing reads it.
        super(messageKey);
        this.status = status;
        this.messageKey = messageKey;
        this.args = args;
    }
}
