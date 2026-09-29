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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Every failure names a machine-readable {@code code}, and every success omits
 * it.
 *
 * <p>The envelope used to carry only a translated sentence, so a client wanting
 * to treat one 409 differently from another had nothing to test but prose that
 * changes with {@code Accept-Language}. {@link #codeIsStableAcrossLocales()} is
 * the contract that makes this worth having — the rest guard the edges.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorCodeTest {

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
	@DisplayName("a success carries no code at all, so existing clients see no change")
	void successHasNoCode() throws Exception {
		mvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.code").doesNotExist());
	}

	@Test
	@DisplayName("a domain error reports the message key it was raised with")
	void domainErrorCarriesItsKey() throws Exception {
		mvc.perform(get("/api/categories/99999999").header("Authorization", token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("error.notFound"));
	}

	/**
	 * The reason this field exists. A client branching on {@code message} breaks
	 * the moment the user switches language; branching on {@code code} does not.
	 */
	@Test
	@DisplayName("the code is identical in Khmer and English while the message is not")
	void codeIsStableAcrossLocales() throws Exception {
		String en = body(get("/api/categories/99999999")
				.header("Authorization", token)
				.header(HttpHeaders.ACCEPT_LANGUAGE, "en"));
		String km = body(get("/api/categories/99999999")
				.header("Authorization", token)
				.header(HttpHeaders.ACCEPT_LANGUAGE, "km"));

		assertThat(json.readTree(en).path("code").asText())
				.isEqualTo(json.readTree(km).path("code").asText())
				.isEqualTo("error.notFound");

		assertThat(json.readTree(en).path("message").asText())
				.as("the two languages must really differ, or this proves nothing")
				.isNotEqualTo(json.readTree(km).path("message").asText());
	}

	@Test
	@DisplayName("a routing failure is coded too, not only domain errors")
	void unknownPathIsCoded() throws Exception {
		mvc.perform(get("/api/definitely-not-a-route").header("Authorization", token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("error.request.noEndpoint"));
	}

	@Test
	@DisplayName("a validation failure is coded, though its message is composed per field")
	void validationIsCoded() throws Exception {
		mvc.perform(post("/api/categories").header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":""}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("error.request.validation"));
	}

	/**
	 * Security rejections never reach {@code @RestControllerAdvice} — they are
	 * written by hand inside the filter chain. That is exactly the kind of
	 * second code path where a new field gets forgotten.
	 */
	@Test
	@DisplayName("a 401 from the filter chain is coded as well")
	void unauthenticatedIsCoded() throws Exception {
		mvc.perform(get("/api/categories"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("error.auth.required"));
	}

	private String body(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req)
			throws Exception {
		return mvc.perform(req).andReturn().getResponse().getContentAsString();
	}
}
