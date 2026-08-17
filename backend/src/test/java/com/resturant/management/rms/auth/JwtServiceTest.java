package com.resturant.management.rms.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for token handling — no Spring context, so these run in
 * milliseconds and cover the cases an HTTP test cannot reach easily
 * (tampering, expiry, a wrong signing key).
 */
class JwtServiceTest {

	private static final String SECRET =
			"a-test-secret-that-is-comfortably-longer-than-thirty-two-bytes";

	private JwtService service(long accessTtl, long refreshTtl) {
		JwtService s = new JwtService(SECRET, accessTtl, refreshTtl);
		s.init();
		return s;
	}

	private JwtService service() {
		return service(3_600_000, 86_400_000);
	}

	@Test
	@DisplayName("an access token round-trips its subject and role")
	void roundTrip() {
		JwtService jwt = service();
		String token = jwt.generateAccessToken("admin", "ADMIN");

		assertThat(jwt.extractUsername(token)).isEqualTo("admin");
		assertThat(jwt.isAccessToken(token)).isTrue();
		assertThat(jwt.isRefreshToken(token)).isFalse();
		assertThat(jwt.isValid(token, true)).isTrue();
	}

	@Test
	@DisplayName("a refresh token is not accepted as an access token")
	void typeConfusionRejected() {
		JwtService jwt = service();
		String refresh = jwt.generateRefreshToken("admin");

		assertThat(jwt.isRefreshToken(refresh)).isTrue();
		assertThat(jwt.isValid(refresh, true)).isFalse();   // asked for access, given refresh
		assertThat(jwt.isValid(refresh, false)).isTrue();
	}

	@Test
	@DisplayName("an expired token is invalid rather than throwing")
	void expiredToken() {
		// Negative TTL puts the expiry in the past immediately.
		JwtService jwt = service(-1000, -1000);
		String token = jwt.generateAccessToken("admin", "ADMIN");

		assertThat(jwt.isValid(token, true)).isFalse();
	}

	@Test
	@DisplayName("a tampered payload fails signature verification")
	void tamperedToken() {
		JwtService jwt = service();
		String token = jwt.generateAccessToken("cashier", "CASHIER");

		// Flip a character in the payload segment.
		String[] parts = token.split("\\.");
		char[] payload = parts[1].toCharArray();
		payload[5] = payload[5] == 'A' ? 'B' : 'A';
		String tampered = parts[0] + "." + new String(payload) + "." + parts[2];

		assertThat(jwt.isValid(tampered, true)).isFalse();
	}

	@Test
	@DisplayName("a token signed with a different key is rejected")
	void wrongSigningKey() {
		JwtService mine = service();
		JwtService theirs = new JwtService(
				"a-completely-different-secret-also-longer-than-thirty-two", 3_600_000, 86_400_000);
		theirs.init();

		String forged = theirs.generateAccessToken("admin", "ADMIN");
		assertThat(mine.isValid(forged, true)).isFalse();
	}

	@Test
	@DisplayName("garbage input is invalid, not an exception")
	void garbageInput() {
		JwtService jwt = service();

		assertThat(jwt.isValid("", true)).isFalse();
		assertThat(jwt.isValid("not.a.jwt", true)).isFalse();
		assertThat(jwt.isValid("aaaa", true)).isFalse();
	}

	@Test
	@DisplayName("startup fails loudly when the secret is missing or too short")
	void refusesWeakSecret() {
		assertThatThrownBy(() -> new JwtService("", 1000, 1000).init())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not set");

		assertThatThrownBy(() -> new JwtService("too-short", 1000, 1000).init())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at least 32 bytes");
	}

	@Test
	@DisplayName("expiry helpers report seconds, not milliseconds")
	void expirySeconds() {
		JwtService jwt = service(3_600_000, 86_400_000);
		assertThat(jwt.getAccessExpirationSeconds()).isEqualTo(3600);
		assertThat(jwt.getRefreshExpirationSeconds()).isEqualTo(86400);
	}
}
