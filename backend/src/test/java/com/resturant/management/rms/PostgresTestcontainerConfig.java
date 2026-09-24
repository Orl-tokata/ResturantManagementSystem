package com.resturant.management.rms;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Supplies a real PostgreSQL 16 for the {@code testpg} profile.
 *
 * <p>{@code @ServiceConnection} wires the container's JDBC URL, username and
 * password into the context automatically, so no datasource properties are
 * hardcoded anywhere.
 *
 * <p>The container is a static singleton: Testcontainers reuses it across every
 * test class in the run rather than starting one per class, which would add
 * minutes to the suite.
 *
 * <p>Active only under {@code testpg}, so the default {@code ./gradlew test}
 * still runs on H2 and needs no Docker daemon.
 *
 * <p>A plain {@code @Configuration} rather than {@code @TestConfiguration}:
 * the latter is deliberately excluded from component scanning, so it would
 * never be picked up without an explicit {@code @Import} on every test class.
 */
@Profile("testpg")
@Configuration
public class PostgresTestcontainerConfig {

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")   // Testcontainers closes it on JVM shutdown
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("rms")
                .withUsername("rms")
                .withPassword("test-only");
    }
}
