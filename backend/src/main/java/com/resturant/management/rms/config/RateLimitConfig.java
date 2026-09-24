package com.resturant.management.rms.config;

import com.resturant.management.rms.security.RateLimiter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    /**
     * A bean rather than Clock.systemUTC() inside the limiter, so a test can
     * move time forward and assert that tokens come back without sleeping for
     * a real minute.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RateLimiter rateLimiter(Clock clock) {
        return new RateLimiter(clock);
    }
}
