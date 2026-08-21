package com.resturant.management.rms.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The errors a user actually reads must come back in their language.
 *
 * <p>Checks the whole chain over real HTTP: the {@code Accept-Language} header
 * reaches the locale resolver, the resolver picks a supported locale, and the
 * exception handler formats the key against it — including the cases that do
 * not go through the handler at all.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ErrorLocaleTest {

	/** Anything in this range is Khmer script; proves the text is not English. */
	private static boolean isKhmer(String s) {
		return s.chars().anyMatch(c -> c >= 0x1780 && c <= 0x17FF);
	}

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
		token = json.readTree(res.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();
	}

	@Test
	@DisplayName("a bad login reads in Khmer when Khmer is asked for")
	void badCredentialsInKhmer() throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.header(HttpHeaders.ACCEPT_LANGUAGE, "km")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"wrong-on-purpose"}"""))
				.andExpect(status().isUnauthorized())
				.andReturn();

		String message = json.readTree(res.getResponse().getContentAsString())
				.path("message").asText();
		assertThat(isKhmer(message)).as("message was '%s'", message).isTrue();
	}

	@Test
	@DisplayName("the same error is English when English is asked for")
	void badCredentialsInEnglish() throws Exception {
		mvc.perform(post("/api/auth/login")
						.header(HttpHeaders.ACCEPT_LANGUAGE, "en")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"wrong-on-purpose"}"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	@DisplayName("no Accept-Language falls back to English, so an unaware caller is unaffected")
	void defaultsToEnglish() throws Exception {
		mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"wrong-on-purpose"}"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	@DisplayName("an unsupported language falls back rather than failing")
	void unsupportedLanguageFallsBack() throws Exception {
		mvc.perform(post("/api/auth/login")
						.header(HttpHeaders.ACCEPT_LANGUAGE, "fr-FR")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"wrong-on-purpose"}"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	/**
	 * A weighted header, which is what a real browser sends. Khmer is preferred
	 * here even though English appears first, so this fails if the resolver is
	 * naively reading the first tag instead of the q-values.
	 */
	@Test
	@DisplayName("q-values decide, not header order")
	void respectsQualityValues() throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.header(HttpHeaders.ACCEPT_LANGUAGE, "en;q=0.6, km;q=0.9")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"admin","password":"wrong-on-purpose"}"""))
				.andExpect(status().isUnauthorized())
				.andReturn();

		String message = json.readTree(res.getResponse().getContentAsString())
				.path("message").asText();
		assertThat(isKhmer(message)).as("message was '%s'", message).isTrue();
	}

	/**
	 * A 404 with an interpolated entity name: the noun inside the sentence has
	 * to be translated too, not left as the English "Category".
	 */
	@Test
	@DisplayName("an interpolated entity name is translated inside the sentence")
	void notFoundTranslatesTheEntityName() throws Exception {
		MvcResult res = mvc.perform(get("/api/categories/999999")
						.header(HttpHeaders.ACCEPT_LANGUAGE, "km")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isNotFound())
				.andReturn();

		String message = json.readTree(res.getResponse().getContentAsString())
				.path("message").asText();
		assertThat(isKhmer(message)).as("message was '%s'", message).isTrue();
		assertThat(message).as("the English entity name leaked through").doesNotContain("Category");
		assertThat(message).as("the id is still reported").contains("999999");
	}

	/**
	 * Bean Validation messages come from Hibernate Validator, not the exception
	 * handler, so they only localize if the validator was wired to our bundle.
	 * The field name is composed separately and has to translate as well.
	 */
	@Test
	@DisplayName("validation messages and field names are both translated")
	void validationInKhmer() throws Exception {
		MvcResult res = mvc.perform(post("/api/categories")
						.header(HttpHeaders.ACCEPT_LANGUAGE, "km")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":""}"""))
				.andExpect(status().isBadRequest())
				.andReturn();

		String message = json.readTree(res.getResponse().getContentAsString())
				.path("message").asText();
		assertThat(isKhmer(message)).as("message was '%s'", message).isTrue();
	}

	/**
	 * The 401 from the security filter chain never reaches
	 * {@code @RestControllerAdvice}, and runs before Spring populates the
	 * request locale. It is the case most likely to be left in English.
	 */
	@Test
	@DisplayName("the filter-chain 401 is translated too")
	void unauthenticatedInKhmer() throws Exception {
		MvcResult res = mvc.perform(get("/api/categories")
						.header(HttpHeaders.ACCEPT_LANGUAGE, "km"))
				.andExpect(status().isUnauthorized())
				.andReturn();

		String message = json.readTree(res.getResponse().getContentAsString())
				.path("message").asText();
		assertThat(isKhmer(message)).as("message was '%s'", message).isTrue();
	}

	/** A delete guard: the longest composed sentence, with three nested nouns. */
	@Test
	@DisplayName("a delete guard translates every noun it splices in")
	void deleteGuardInKhmer() throws Exception {
		MvcResult res = mvc.perform(delete("/api/categories/1")
						.header(HttpHeaders.ACCEPT_LANGUAGE, "km")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isConflict())
				.andReturn();

		String message = json.readTree(res.getResponse().getContentAsString())
				.path("message").asText();
		assertThat(isKhmer(message)).as("message was '%s'", message).isTrue();
		assertThat(message)
				.as("an English fragment survived: %s", message)
				.doesNotContain("Cannot delete")
				.doesNotContain("product(s)")
				.doesNotContain("Category");
	}
}
