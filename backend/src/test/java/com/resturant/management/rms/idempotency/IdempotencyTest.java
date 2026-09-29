package com.resturant.management.rms.idempotency;

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
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * A cashier taps Pay twice on a weak connection and gets one bill.
 *
 * <p>{@code @Transactional} rolls the orders back as usual, but rows in
 * {@code idempotency_key} are written in their own transaction — that is the
 * point of them — so they survive the rollback. Every key here is a fresh UUID
 * so the leftovers cannot collide with anything.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdempotencyTest {

	private static final String KEY = "Idempotency-Key";

	/** Seeded FREE tables, so opening an order on one succeeds. */
	private static final long FREE_TABLE = 6;
	private static final long OTHER_FREE_TABLE = 8;

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;

	private String token;
	private String adminToken;

	@BeforeEach
	void signIn() throws Exception {
		token = "Bearer " + login("cashier");
		adminToken = "Bearer " + login("admin");
	}

	private String login(String username) throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"%s\",\"password\":\"ChangeMe123!\"}".formatted(username)))
				.andReturn();
		return json.readTree(res.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();
	}

	private static String key() {
		return UUID.randomUUID().toString();
	}

	private MvcResult openOrder(String idempotencyKey, long tableId) throws Exception {
		return mvc.perform(post("/api/orders")
						.header("Authorization", token)
						.header(KEY, idempotencyKey)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tableId\":%d,\"guestCount\":2}".formatted(tableId)))
				.andReturn();
	}

	private String invoiceOf(MvcResult res) throws Exception {
		return json.readTree(res.getResponse().getContentAsString())
				.path("data").path("invoiceNo").asText();
	}

	/* ---- The contract ------------------------------------------------------ */

	/**
	 * The reason all of this exists. Two identical requests, one order — proven
	 * by the invoice number, which is generated per order and would differ if a
	 * second had really been opened.
	 */
	@Test
	@DisplayName("the same key twice returns the first reply, not a second order")
	void repeatReturnsTheOriginal() throws Exception {
		String k = key();

		MvcResult first = openOrder(k, FREE_TABLE);
		assertThat(first.getResponse().getStatus()).isEqualTo(201);

		MvcResult second = openOrder(k, FREE_TABLE);

		assertThat(second.getResponse().getStatus())
				.as("a replay repeats the original status, not a fresh 201 from new work")
				.isEqualTo(201);
		assertThat(invoiceOf(second)).isEqualTo(invoiceOf(first));
		assertThat(second.getResponse().getHeader("Idempotent-Replay")).isEqualTo("true");
		assertThat(first.getResponse().getHeader("Idempotent-Replay"))
				.as("the original is not a replay")
				.isNull();
	}

	@Test
	@DisplayName("two different keys really do open two orders")
	void differentKeysAreNotDeduplicated() throws Exception {
		// Guards the test above: if opening an order were somehow failing, it
		// would still "pass" by returning the same thing twice.
		String a = invoiceOf(openOrder(key(), FREE_TABLE));
		String b = invoiceOf(openOrder(key(), OTHER_FREE_TABLE));
		assertThat(a).isNotEqualTo(b);
	}

	/* ---- The header itself -------------------------------------------------- */

	@Test
	@DisplayName("a protected endpoint refuses a request with no key")
	void missingKeyIsRejected() throws Exception {
		mvc.perform(post("/api/orders")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tableId\":%d,\"guestCount\":2}".formatted(FREE_TABLE)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("error.idempotency.required"));
	}

	@Test
	@DisplayName("an over-long key is refused rather than truncated into the column")
	void overLongKeyIsRejected() throws Exception {
		mvc.perform(post("/api/orders")
						.header("Authorization", token)
						.header(KEY, "x".repeat(200))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tableId\":%d,\"guestCount\":2}".formatted(FREE_TABLE)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("error.idempotency.tooLong"));
	}

	/**
	 * The list is deliberately short — the endpoints that move money or stock,
	 * not every POST. A catalog edit repeated by accident is a visible nuisance
	 * someone fixes in a moment, and making it demand a header would be friction
	 * bought for nothing.
	 */
	@Test
	@DisplayName("a POST that is not on the list still works with no key at all")
	void unlistedPostNeedsNoKey() throws Exception {
		mvc.perform(post("/api/categories")
						.header("Authorization", adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"ចំណីសាកល្បង","nameEn":"Idempotency probe"}"""))
				.andExpect(status().isCreated());
	}

	/* ---- Misuse ------------------------------------------------------------- */

	@Test
	@DisplayName("reusing a key on a different endpoint is refused, not answered wrongly")
	void keyReusedElsewhereIsRefused() throws Exception {
		String k = key();
		long orderId = json.readTree(openOrder(k, FREE_TABLE).getResponse().getContentAsString())
				.path("data").path("id").asLong();

		mvc.perform(post("/api/orders/%d/cancel".formatted(orderId))
						.header("Authorization", token)
						.header(KEY, k))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("error.idempotency.mismatch"));
	}

	/**
	 * A rejected request did nothing, so its key must not be spent. Otherwise a
	 * cashier who mistypes once could never retry that intent — the system would
	 * answer the corrected request with the original complaint.
	 */
	@Test
	@DisplayName("a failed request frees its key for a corrected retry")
	void failureReleasesTheKey() throws Exception {
		String k = key();

		mvc.perform(post("/api/orders")
						.header("Authorization", token)
						.header(KEY, k)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"tableId":99999,"guestCount":2}"""))
				.andExpect(status().isNotFound());

		// Same key, corrected body.
		assertThat(openOrder(k, FREE_TABLE).getResponse().getStatus())
				.as("the key was released, so this is new work rather than a replay or a 409")
				.isEqualTo(201);
	}
}
