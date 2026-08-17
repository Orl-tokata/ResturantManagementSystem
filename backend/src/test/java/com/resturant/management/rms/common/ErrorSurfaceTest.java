package com.resturant.management.rms.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Milestone 14 — every error a client can provoke must come back in the
 * {@code ApiResponse} shape with an honest status code.
 *
 * <p>These exist because all four of the cases below previously returned 500:
 * the catch-all handler was swallowing them, so a URL typo told the caller the
 * server had broken.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorSurfaceTest {

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;

	private String token;

	@BeforeEach
	void signIn() throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"ChangeMe123!"}"""))
				.andReturn();
		token = "Bearer " + json.readTree(res.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();
	}

	@Test
	@DisplayName("an unknown path is 404, not 500")
	void unknownPath() throws Exception {
		mvc.perform(get("/api/definitely-not-a-route").header("Authorization", token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("No endpoint matches this path"));
	}

	@Test
	@DisplayName("the wrong HTTP method is 405 and names what is allowed")
	void wrongMethod() throws Exception {
		mvc.perform(delete("/api/health").header("Authorization", token))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("Allowed")));
	}

	@Test
	@DisplayName("a non-JSON content type is 415")
	void wrongContentType() throws Exception {
		mvc.perform(post("/api/categories").header("Authorization", token)
						.contentType(MediaType.TEXT_PLAIN)
						.content("not json"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.status").value(415))
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("application/json")));
	}

	@Test
	@DisplayName("a missing required query parameter is 400 and names the parameter")
	void missingParam() throws Exception {
		mvc.perform(get("/api/orders/open").header("Authorization", token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("tableId")));
	}

	@Test
	@DisplayName("a non-numeric path id is 400, not 500")
	void badPathVariable() throws Exception {
		mvc.perform(get("/api/products/not-a-number").header("Authorization", token))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("errors never leak a stack trace or exception class name")
	void noStackTraceLeak() throws Exception {
		MvcResult res = mvc.perform(get("/api/definitely-not-a-route").header("Authorization", token))
				.andReturn();
		String body = res.getResponse().getContentAsString();

		org.assertj.core.api.Assertions.assertThat(body)
				.doesNotContain("Exception")
				.doesNotContain("org.springframework")
				.doesNotContain("com.resturant")
				.doesNotContain("at java.");
	}

	@Test
	@DisplayName("security headers are present on API responses")
	void securityHeaders() throws Exception {
		mvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
				.andExpect(header().string("Content-Security-Policy",
						org.hamcrest.Matchers.containsString("default-src 'self'")))
				.andExpect(header().string("Permissions-Policy",
						org.hamcrest.Matchers.containsString("camera=()")));
	}

	@Test
	@DisplayName("an unauthenticated request to an unknown path is still 401, not 404")
	void unknownPathStillRequiresAuth() throws Exception {
		// Order matters: authorisation runs before handler lookup, so an anonymous
		// probe must not be able to enumerate which paths exist.
		mvc.perform(get("/api/definitely-not-a-route"))
				.andExpect(status().isUnauthorized());
	}
}
