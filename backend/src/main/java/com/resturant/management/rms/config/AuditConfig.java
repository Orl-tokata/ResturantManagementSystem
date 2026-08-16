package com.resturant.management.rms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Supplies the value written into {@code reg_id} / {@code mod_id}.
 *
 * <p>Falls back to {@code "system"} for startup seeding and any unauthenticated
 * path, so audit columns are never null.
 */
@Configuration
public class AuditConfig {

    private static final String SYSTEM = "system";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of(SYSTEM);
            }
            return Optional.ofNullable(auth.getName()).or(() -> Optional.of(SYSTEM));
        };
    }
}
