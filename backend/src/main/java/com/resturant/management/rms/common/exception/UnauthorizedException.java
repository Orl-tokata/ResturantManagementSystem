package com.resturant.management.rms.common.exception;

import org.springframework.http.HttpStatus;

/**
 * The caller is not authenticated, or no longer is.
 *
 * <p>Distinct from {@link BadRequestException}: a missing or expired refresh
 * token is not a malformed request, it is an absent session. The difference
 * matters to whoever reads the response — 400 invites them to look for a bug
 * in what they sent, 401 tells them to sign in.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String messageKey, Object... args) {
        super(HttpStatus.UNAUTHORIZED, messageKey, args);
    }
}
