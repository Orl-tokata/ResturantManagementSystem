package com.resturant.management.rms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.i18n.Messages;
import com.resturant.management.rms.config.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Caps how often one caller may hit the endpoints that need no token.
 *
 * <p>Account lockout already stops brute force against a single account, but it
 * is the wrong shape for two other attacks: credential stuffing walks a list of
 * accounts so no single one ever reaches five failures, and an attacker who
 * knows a username can lock a real person out on purpose by failing five times
 * for them. A limit per caller covers both, and it is the only thing that
 * covers guessing a six-digit OTP.
 *
 * <p>Runs before authentication, so it costs an attacker a rejected request
 * rather than a password hash comparison — which is the expensive part of a
 * login and the reason a flood hurts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final Messages messages;
    private final LocaleResolver localeResolver;

    /** Only endpoints reachable without a token; everything else needs one. */
    private Map<String, RateLimitProperties.Rule> rules() {
        return Map.of(
                "/api/auth/login", properties.login(),
                "/api/auth/forgot-password", properties.forgotPassword(),
                "/api/auth/verify-otp", properties.verifyOtp(),
                "/api/auth/register", properties.register());
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // A preflight carries no credentials and is not a guess at anything.
        return !properties.enabled()
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !rules().containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        RateLimitProperties.Rule rule = rules().get(path);
        String client = clientId(request);

        // Keyed by path as well as caller, so spending the login allowance does
        // not also block a password reset — they are different attacks with
        // different budgets.
        RateLimiter.Decision decision =
                rateLimiter.tryConsume(path + "|" + client, rule.limit(), rule.window());

        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit hit: {} from {} ({} per {})",
                path, client, rule.limit(), rule.window());
        reject(request, response, decision.retryAfterSeconds());
    }

    /**
     * Who to charge for this request.
     *
     * <p>{@code X-Forwarded-For} is only consulted when configured, because it
     * is caller supplied: trusting it with nothing upstream rewriting it means
     * an attacker sends a fresh value per request and is never limited. Behind
     * a proxy the opposite is true — every request appears to come from the
     * proxy, and one caller's flood would lock out everyone. Hence the switch,
     * and hence taking the first entry, which is the original client.
     */
    private String clientId(HttpServletRequest request) {
        if (properties.trustForwardedFor()) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        String address = request.getRemoteAddr();
        return address == null ? "unknown" : address;
    }

    /**
     * Shaped here rather than by the exception handler: this filter runs inside
     * the chain, where @RestControllerAdvice never sees it, and before Spring
     * resolves the request locale — so both the envelope and the language have
     * to be done by hand, the same way SecurityConfig does for its 401.
     */
    private void reject(HttpServletRequest request, HttpServletResponse response, long retryAfter)
            throws IOException {
        Locale locale = localeResolver.resolveLocale(request);

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.error(429, messages.get(locale, "error.request.tooManyRequests", retryAfter)));
    }
}
