package com.resturant.management.rms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * The caller is who they say they are and still may not do this.
 *
 * <p>Distinct from Spring's {@code AccessDeniedException}, which the handler
 * answers with one generic message, because some refusals have to say what the
 * rule was: a refund above the approval threshold is not a mistake the cashier
 * can fix by trying again, it is a sentence telling them to fetch a manager.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String messageKey, Object... args) {
        super(HttpStatus.FORBIDDEN, messageKey, args);
    }
}
