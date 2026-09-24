package com.resturant.management.rms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Limits for the endpoints an unauthenticated caller can reach.
 *
 * @param enabled            off in dev and in tests — see application-dev.yml
 * @param trustForwardedFor  whether {@code X-Forwarded-For} may name the
 *                           caller. Defaults to false, and that default is the
 *                           security-relevant part: the header is caller
 *                           supplied, so trusting it when nothing upstream
 *                           rewrites it lets an attacker send a different
 *                           value per request and never hit a limit at all.
 *                           Turn it on only behind a proxy that overwrites it.
 * @param login              credential stuffing against many accounts
 * @param forgotPassword     mail flooding, and probing which emails exist
 * @param verifyOtp          the tightest: a six-digit code is a million
 *                           guesses, which is minutes of unthrottled traffic
 * @param register           account spam
 */
@ConfigurationProperties(prefix = "app.security.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("false") boolean trustForwardedFor,
        @DefaultValue Rule login,
        @DefaultValue Rule forgotPassword,
        @DefaultValue Rule verifyOtp,
        @DefaultValue Rule register) {

    /**
     * @param limit  requests allowed per window from one caller
     * @param window how long a spent allowance takes to come back
     */
    public record Rule(
            @DefaultValue("10") int limit,
            @DefaultValue("1m") Duration window) {
    }
}
