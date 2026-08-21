package com.resturant.management.rms.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends ApiException {

    public NotFoundException(String messageKey, Object... args) {
        super(HttpStatus.NOT_FOUND, messageKey, args);
    }

    /**
     * The common shape: "<entity> not found: <id>".
     *
     * <p>{@code entityKey} is itself a message key, so the entity name is
     * translated too — otherwise a Khmer sentence would carry the English word
     * "Category" in the middle of it.
     */
    public static NotFoundException of(String entityKey, Object id) {
        return new NotFoundException("error.notFound", new LocalizedArg(entityKey), id);
    }
}
