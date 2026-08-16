package com.resturant.management.rms.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end checks over the real filter chain: routes, JWT issuing and
 * verification, role authorities, lockout, and the public/private split.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthControllerTest {

	private static final String ADMIN = """
			{"username":"admin","password":"ChangeMe123!"}""";

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;

	/* ---- Login ---------------------------------------------------------- */

	@Test
	@DisplayName("login returns an access token and sets an httpOnly refresh cookie")
	void loginSucceeds() throws Exception {
		MvcResult result = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON).content(ADMIN))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.user.username").value("admin"))
				.andExpect(jsonPath("$.data.user.role").value("ADMIN"))
				.andReturn();

		String setCookie = result.getResponse().getHeader("Set-Cookie");
		assertThat(setCookie).contains("rms_refresh=").contains("HttpOnly").contains("Path=/api/auth");
	}

	@Test
	@DisplayName("login with a wrong password returns 401 without revealing which field was wrong")
	void loginWrongPassword() throws Exception {
		mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"NotThePassword1"}"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	@DisplayName("login with an unknown user gives the same 401 message")
	void loginUnknownUser() throws Exception {
		mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"nobody","password":"Whatever123"}"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	@DisplayName("login rejects a blank username with a field-level message")
	void loginValidation() throws Exception {
		mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"","password":""}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("username")));
	}

	/* ---- Protected routes ------------------------------------------------ */

	@Test
	@DisplayName("/me without a token is 401 in the ApiResponse shape")
	void meRequiresAuth() throws Exception {
		mvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Authentication required"));
	}

	@Test
	@DisplayName("/me with a valid token returns the current user")
	void meWithToken() throws Exception {
		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + login()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.username").value("admin"))
				.andExpect(jsonPath("$.data.role").value("ADMIN"));
	}

	@Test
	@DisplayName("a garbage bearer token is rejected, not a 500")
	void garbageTokenRejected() throws Exception {
		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not.a.jwt"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("change-password requires authentication — it must not be public just because it sits under /api/auth")
	void changePasswordRequiresAuth() throws Exception {
		mvc.perform(post("/api/auth/change-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currentPassword":"ChangeMe123!","newPassword":"Different123"}"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("change-password rejects a wrong current password")
	void changePasswordWrongCurrent() throws Exception {
		mvc.perform(post("/api/auth/change-password")
						.header("Authorization", "Bearer " + login())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currentPassword":"WrongPassword1","newPassword":"Different123"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Current password is incorrect"));
	}

	@Test
	@DisplayName("a refresh token cannot be replayed as an access token")
	void refreshTokenIsNotAnAccessToken() throws Exception {
		MvcResult result = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON).content(ADMIN))
				.andReturn();

		String cookie = result.getResponse().getHeader("Set-Cookie");
		String refresh = cookie.substring(cookie.indexOf('=') + 1, cookie.indexOf(';'));

		mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + refresh))
				.andExpect(status().isUnauthorized());
	}

	/* ---- Refresh --------------------------------------------------------- */

	@Test
	@DisplayName("refresh without the cookie is a 400, not a 500")
	void refreshWithoutCookie() throws Exception {
		mvc.perform(post("/api/auth/refresh"))
				.andExpect(status().isBadRequest());
	}

	/* ---- Registration ---------------------------------------------------- */

	@Test
	@DisplayName("register creates a user and rejects a duplicate username with 409")
	void registerThenConflict() throws Exception {
		String body = """
				{"username":"waiter1","password":"Passw0rdX","fullName":"Kim Srey Neat",
				 "email":"waiter1@rms.local","phone":"016 555 777","role":"WAITER"}""";

		mvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.username").value("waiter1"))
				.andExpect(jsonPath("$.data.role").value("WAITER"));

		mvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("register enforces the password policy")
	void registerWeakPassword() throws Exception {
		mvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"weakuser","password":"alllowercase","fullName":"Weak"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("password")));
	}

	/* ---- Forgot password -------------------------------------------------- */

	@Test
	@DisplayName("forgot-password reports success for an unknown email so addresses cannot be enumerated")
	void forgotPasswordDoesNotLeak() throws Exception {
		mvc.perform(post("/api/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"definitely-not-registered@example.com"}"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200));
	}

	@Test
	@DisplayName("verify-otp rejects a wrong code")
	void verifyOtpRejectsWrongCode() throws Exception {
		mvc.perform(post("/api/auth/forgot-password")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"admin@rms.local"}"""))
				.andExpect(status().isOk());

		mvc.perform(post("/api/auth/verify-otp")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"admin@rms.local","code":"000000"}"""))
				.andExpect(status().isBadRequest());
	}

	/* ---- Public endpoints stay public -------------------------------------- */

	@Test
	@DisplayName("health stays reachable without a token")
	void healthIsPublic() throws Exception {
		mvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("UP"));
	}

	/* ---- Helper ------------------------------------------------------------ */

	private String login() throws Exception {
		MvcResult result = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON).content(ADMIN))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode node = json.readTree(result.getResponse().getContentAsString());
		return node.path("data").path("accessToken").asText();
	}
}
