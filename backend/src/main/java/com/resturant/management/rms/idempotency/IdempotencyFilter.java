package com.resturant.management.rms.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.i18n.Messages;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Stops one intent becoming two records.
 *
 * <p>A till on a weak connection is the ordinary case, not the exceptional one:
 * the cashier taps Pay, sees nothing happen, and taps again. Without this the
 * second tap is a second bill — and the first anyone knows of it is a stock
 * count that will not reconcile.
 *
 * <p>The caller sends {@code Idempotency-Key} with a value of its own choosing,
 * one per intent. A repeat of the same key returns the first reply rather than
 * doing the work twice.
 *
 * <p>A header rather than a body field: it describes the request, not the
 * payment, and the same mechanism has to work for endpoints whose bodies have
 * nothing in common.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    /**
     * Where a repeat would do real damage.
     *
     * <p>Deliberately a short, explicit list rather than "every POST". Most
     * posts here are catalog edits where a duplicate is a visible nuisance a
     * user fixes in a moment; these are the ones that move money or stock,
     * where a duplicate is a wrong number nobody sees until later.
     *
     * <p>Grows with the packages in docs/PLAN.md — payments, returns and shift
     * close are on it as they land.
     */
    private static final List<String> REQUIRED = List.of(
            "POST /api/orders",
            "POST /api/orders/*/pay",
            "POST /api/orders/*/cancel",
            "POST /api/stock/*/adjust",
            "POST /api/purchases/*/receive");

    private static final String HEADER = "Idempotency-Key";
    private static final String REPLAY_HEADER = "Idempotent-Replay";
    private static final int MAX_KEY_LENGTH = 80;

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final IdempotencyService service;
    private final ObjectMapper objectMapper;
    private final Messages messages;
    private final LocaleResolver localeResolver;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !isRequired(request.getMethod(), request.getRequestURI());
    }

    private boolean isRequired(String method, String path) {
        String candidate = method + " " + path;
        return REQUIRED.stream().anyMatch(pattern -> matcher.match(pattern, candidate));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String key = request.getHeader(HEADER);
        if (key == null || key.isBlank()) {
            reject(request, response, HttpStatus.BAD_REQUEST, "error.idempotency.required");
            return;
        }
        if (key.length() > MAX_KEY_LENGTH) {
            reject(request, response, HttpStatus.BAD_REQUEST, "error.idempotency.tooLong",
                    MAX_KEY_LENGTH);
            return;
        }

        String user = request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName();
        IdempotencyService.Claim claim =
                service.claim(key.trim(), request.getMethod(), request.getRequestURI(), user);

        switch (claim.outcome()) {
            case REPLAY -> replay(response, claim);
            case IN_PROGRESS -> reject(request, response, HttpStatus.CONFLICT,
                    "error.idempotency.inProgress");
            case MISMATCH -> reject(request, response, HttpStatus.CONFLICT,
                    "error.idempotency.mismatch");
            case UNREPLAYABLE -> reject(request, response, HttpStatus.CONFLICT,
                    "error.idempotency.unreplayable");
            case PROCEED -> proceed(request, response, chain, key.trim());
        }
    }

    /**
     * Runs the request, then records its reply.
     *
     * <p>The response is wrapped so the bytes written by the controller can be
     * read back here. {@code copyBodyToResponse} is what actually sends them —
     * without it the wrapper holds the body and the client receives nothing,
     * which is a silent and thoroughly confusing failure.
     */
    private void proceed(HttpServletRequest request, HttpServletResponse response,
                         FilterChain chain, String key) throws ServletException, IOException {

        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        boolean handled = false;
        try {
            chain.doFilter(request, wrapper);
            handled = true;

            int status = wrapper.getStatus();
            String body = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);

            if (status >= 200 && status < 300) {
                service.complete(key, status, body);
            } else {
                // A rejected request did nothing, so the key should not be spent.
                service.release(key);
            }
        } finally {
            if (!handled) {
                // The handler threw past the exception advice. Nothing was
                // committed, so free the key rather than leaving it claimed
                // forever and blocking every retry.
                service.release(key);
            }
            wrapper.copyBodyToResponse();
        }
    }

    private void replay(HttpServletResponse response, IdempotencyService.Claim claim)
            throws IOException {
        response.setStatus(claim.status());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        // Not part of the contract, but it turns "why did nothing happen?" into
        // a one-second answer when someone is reading a network tab.
        response.setHeader(REPLAY_HEADER, "true");
        response.getOutputStream().write(claim.body().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Filters run outside {@code @RestControllerAdvice}, so the envelope and the
     * language are both assembled here — the same way {@code RateLimitFilter}
     * does for its 429.
     */
    private void reject(HttpServletRequest request, HttpServletResponse response,
                        HttpStatus status, String key, Object... args) throws IOException {
        Locale locale = localeResolver.resolveLocale(request);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.error(status.value(), key, messages.get(locale, key, args)));
    }
}
