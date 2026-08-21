package com.resturant.management.rms.config;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.i18n.Messages;
import com.resturant.management.rms.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize on service and controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Listed one by one rather than as {@code /api/auth/**}: that wildcard would
     * also expose {@code /me} and {@code /change-password}, which need a
     * principal. Anything added under {@code /api/auth} is private by default.
     */
    private static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/forgot-password",
            "/api/auth/verify-otp",
            "/api/auth/reset-password",
            "/api/health/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/h2-console/**"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final ObjectMapper objectMapper;
    private final Messages messages;
    private final LocaleResolver localeResolver;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Safe to disable: the API is stateless and authenticated by a bearer
                // token, not by an ambient session cookie.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                write(req, res, HttpServletResponse.SC_UNAUTHORIZED,
                                        "error.auth.required"))
                        .accessDeniedHandler((req, res, e) ->
                                write(req, res, HttpServletResponse.SC_FORBIDDEN,
                                        "error.auth.forbidden"))
                )
                .headers(h -> h
                        // The H2 console renders inside a frame; only reachable on dev.
                        .frameOptions(f -> f.sameOrigin())
                        .referrerPolicy(r -> r.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
                                        .ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Nothing loads from a third-party origin. Not `default-src 'none'`,
                        // because this app also serves Swagger UI, which needs its own
                        // scripts and inline bootstrap; 'none' would leave the docs blank.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self' 'unsafe-inline'; "
                                        + "style-src 'self' 'unsafe-inline'; "
                                        + "img-src 'self' data:; "
                                        + "connect-src 'self'; "
                                        + "object-src 'none'; "
                                        + "base-uri 'self'; "
                                        + "frame-ancestors 'self'"))
                        // Only sent over HTTPS; harmless on plain http locally.
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .permissionsPolicyHeader(p ->
                                p.policy("camera=(), microphone=(), geolocation=()")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Errors raised by the security filters bypass @RestControllerAdvice, so
     * shape them here too — including resolving the message.
     *
     * <p>The locale has to be resolved from the request by hand. These handlers
     * run inside the filter chain, before DispatcherServlet populates
     * LocaleContextHolder, so asking Messages for "the current locale" here
     * would silently answer with the default on every 401. Reusing the same
     * LocaleResolver bean keeps the supported-locale matching identical to the
     * rest of the app.
     */
    private void write(HttpServletRequest request, HttpServletResponse response,
                       int status, String messageKey) throws java.io.IOException {
        Locale locale = localeResolver.resolveLocale(request);
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        objectMapper.writeValue(response.getOutputStream(),
                ApiResponse.error(status, messages.get(locale, messageKey)));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
