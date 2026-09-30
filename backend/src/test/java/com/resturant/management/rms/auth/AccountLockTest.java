package com.resturant.management.rms.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resturant.management.rms.user.UserInfm;
import com.resturant.management.rms.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Five wrong passwords lock an account; fifteen minutes or an administrator
 * unlocks it.
 *
 * <p>Not {@code @Transactional}: the lock is the point, and it is written by
 * the login path in its own transaction. A test that rolled back would assert
 * against state the server never kept. The cashier account is restored in
 * {@link #restoreCashier()} instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountLockTest {

	private static final String TARGET = "cashier";

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;
	@Autowired UserRepository users;

	private String adminToken;

	@BeforeEach
	void signInAsAdmin() throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"ChangeMe123!"}"""))
				.andReturn();
		adminToken = "Bearer " + json.readTree(res.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();
	}

	@AfterEach
	void restoreCashier() {
		users.findByUserId(TARGET).ifPresent(u -> {
			u.unlock();
			users.save(u);
		});
	}

	private int attemptLogin(String password) throws Exception {
		return mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(TARGET, password)))
				.andReturn().getResponse().getStatus();
	}

	private UserInfm reload() {
		return users.findByUserId(TARGET).orElseThrow();
	}

	/* ---- Locking ----------------------------------------------------------- */

	@Test
	@DisplayName("five wrong passwords lock the account, and the fifth says so")
	void locksAfterFiveFailures() throws Exception {
		for (int i = 1; i <= 4; i++) {
			assertThat(attemptLogin("wrong-" + i))
					.as("attempt %d is still just a bad password", i)
					.isEqualTo(401);
		}
		assertThat(attemptLogin("wrong-5")).isEqualTo(401);

		// The sixth attempt meets the lock, which is a different answer from a
		// wrong password — 423, so a client can tell them apart.
		assertThat(attemptLogin("wrong-6")).isEqualTo(423);
		assertThat(reload().isLocked()).isTrue();
	}

	@Test
	@DisplayName("the correct password is refused too while the account is locked")
	void locksOutEvenTheRightPassword() throws Exception {
		for (int i = 1; i <= 5; i++) attemptLogin("wrong-" + i);

		assertThat(attemptLogin("ChangeMe123!"))
				.as("a lock that the real password walks through is not a lock")
				.isEqualTo(423);
	}

	@Test
	@DisplayName("a successful login clears the count before it reaches five")
	void successResetsTheCounter() throws Exception {
		for (int i = 1; i <= 3; i++) attemptLogin("wrong-" + i);
		assertThat(reload().getLoginFailedCnt()).isEqualTo(3);

		assertThat(attemptLogin("ChangeMe123!")).isEqualTo(200);
		assertThat(reload().getLoginFailedCnt()).isZero();
	}

	/* ---- Expiry ------------------------------------------------------------ */

	/**
	 * The reason locks expire at all. Before this, five wrong guesses took a
	 * till out of service permanently — and anyone who knew a cashier's
	 * username could do it deliberately, during service.
	 */
	@Test
	@DisplayName("a lock whose time has passed lets the user back in")
	void automaticLockExpires() throws Exception {
		for (int i = 1; i <= 5; i++) attemptLogin("wrong-" + i);
		assertThat(attemptLogin("ChangeMe123!")).isEqualTo(423);

		// Wind the clock forward by moving the deadline into the past, rather
		// than waiting a quarter of an hour.
		UserInfm locked = reload();
		locked.setLockedUntil(LocalDateTime.now().minusSeconds(1));
		users.save(locked);

		assertThat(attemptLogin("ChangeMe123!"))
				.as("the fifteen minutes are up")
				.isEqualTo(200);
		assertThat(reload().isLocked()).isFalse();
		assertThat(reload().getLoginFailedCnt()).isZero();
	}

	@Test
	@DisplayName("a lock with no deadline is an administrator's and does not expire")
	void manualLockDoesNotExpire() {
		UserInfm user = reload();
		user.setLockYn("Y");
		user.setLockedUntil(null);
		users.save(user);

		assertThat(reload().isLocked())
				.as("null lockedUntil means someone decided this, not the clock")
				.isTrue();
	}

	/* ---- Unlocking --------------------------------------------------------- */

	@Test
	@DisplayName("an administrator can unlock, which is what the error message promises")
	void adminCanUnlock() throws Exception {
		for (int i = 1; i <= 5; i++) attemptLogin("wrong-" + i);
		long id = reload().getId();

		mvc.perform(post("/api/users/" + id + "/unlock").header("Authorization", adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.locked").value(false))
				.andExpect(jsonPath("$.data.failedAttempts").value(0));

		assertThat(attemptLogin("ChangeMe123!")).isEqualTo(200);
	}

	@Test
	@DisplayName("unlocking an account that is not locked is harmless")
	void unlockIsIdempotent() throws Exception {
		long id = reload().getId();
		for (int i = 0; i < 2; i++) {
			mvc.perform(post("/api/users/" + id + "/unlock").header("Authorization", adminToken))
					.andExpect(status().isOk());
		}
	}

	@Test
	@DisplayName("a cashier cannot unlock anybody, including themselves")
	void onlyAdminsMayUnlock() throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"cashier","password":"ChangeMe123!"}"""))
				.andReturn();
		String cashier = "Bearer " + json.readTree(res.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();

		mvc.perform(post("/api/users/" + reload().getId() + "/unlock")
						.header("Authorization", cashier))
				.andExpect(status().isForbidden());
	}

	/* ---- The list ---------------------------------------------------------- */

	@Test
	@DisplayName("the account list reports the lock and never a password")
	void listShowsLockStateAndNoSecrets() throws Exception {
		for (int i = 1; i <= 5; i++) attemptLogin("wrong-" + i);

		MvcResult res = mvc.perform(get("/api/users").header("Authorization", adminToken))
				.andExpect(status().isOk())
				.andReturn();
		String body = res.getResponse().getContentAsString();

		assertThat(body).contains("\"locked\":true");
		assertThat(body)
				.as("a hash is still a credential; it must not leave the entity")
				.doesNotContain("userPwd")
				.doesNotContain("$2a$")
				.doesNotContain("$2b$");
	}

	@Test
	@DisplayName("a cashier cannot read the account list")
	void listIsAdminOnly() throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"cashier","password":"ChangeMe123!"}"""))
				.andReturn();
		String cashier = "Bearer " + json.readTree(res.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();

		mvc.perform(get("/api/users").header("Authorization", cashier))
				.andExpect(status().isForbidden());
	}
}
