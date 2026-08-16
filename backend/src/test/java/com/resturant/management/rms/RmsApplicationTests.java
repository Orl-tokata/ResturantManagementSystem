package com.resturant.management.rms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Context load smoke test.
 *
 * <p>Requires a reachable PostgreSQL instance ({@code docker compose up -d db}).
 * Milestone 14 should replace this with Testcontainers so CI does not depend on
 * a developer's local database.
 */
@SpringBootTest
class RmsApplicationTests {

	@Test
	void contextLoads() {
	}
}
