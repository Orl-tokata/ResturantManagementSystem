package com.resturant.management.rms.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Milestone 9 — the payment transaction and the receipt projection. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional   // roll back after each test: one H2 instance is shared by every test class
class PaymentControllerTest {

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;

	private String token;

	@BeforeEach
	void signIn() throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"cashier","password":"ChangeMe123!"}"""))
				.andReturn();
		token = json.readTree(res.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();
	}

	/** Opens a bill at the given table with 2 × fried rice = 9.00 → total 9.90. */
	private long billWithFriedRice(long tableId) throws Exception {
		MvcResult opened = mvc.perform(post("/api/orders")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tableId\":%d,\"guestCount\":2}".formatted(tableId)))
				.andExpect(status().isCreated())
				.andReturn();
		long id = json.readTree(opened.getResponse().getContentAsString())
				.path("data").path("id").asLong();

		mvc.perform(put("/api/orders/" + id + "/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items":[{"productId":1,"qty":2}]}"""))
				.andExpect(status().isOk());
		return id;
	}

	private JsonNode pay(long id, String body) throws Exception {
		MvcResult res = mvc.perform(post("/api/orders/" + id + "/pay")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andReturn();
		return json.readTree(res.getResponse().getContentAsString()).path("data");
	}

	private double stockOf(long productId) throws Exception {
		MvcResult res = mvc.perform(get("/api/products/" + productId)
				.header("Authorization", "Bearer " + token)).andReturn();
		return json.readTree(res.getResponse().getContentAsString())
				.path("data").path("stockQty").asDouble();
	}

	/* ---- Cash ------------------------------------------------------------- */

	@Test
	@DisplayName("cash payment records the tender and works out the change")
	void cashPaymentComputesChange() throws Exception {
		long id = billWithFriedRice(1);

		JsonNode paid = pay(id, """
				{"paymentMethod":"CASH","amountTendered":20.00}""");

		assertThat(paid.path("status").asText()).isEqualTo("PAID");
		assertThat(paid.path("total").asDouble()).isEqualTo(9.90);      // 9.00 + 10% VAT
		assertThat(paid.path("amountTendered").asDouble()).isEqualTo(20.00);
		assertThat(paid.path("changeAmount").asDouble()).isEqualTo(10.10);
		assertThat(paid.path("paidAt").isNull()).isFalse();
	}

	@Test
	@DisplayName("paying decrements stock for every line")
	void paymentDecrementsStock() throws Exception {
		double before = stockOf(1);
		long id = billWithFriedRice(3);

		pay(id, """
				{"paymentMethod":"CASH","amountTendered":10.00}""");

		assertThat(stockOf(1)).isEqualTo(before - 2);
	}

	@Test
	@DisplayName("paying releases the table")
	void paymentFreesTable() throws Exception {
		long id = billWithFriedRice(4);

		mvc.perform(get("/api/tables/4").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.data.status").value("OCCUPIED"));

		pay(id, """
				{"paymentMethod":"CASH","amountTendered":10.00}""");

		mvc.perform(get("/api/tables/4").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.data.status").value("FREE"));
	}

	@Test
	@DisplayName("cash under the total is rejected and nothing is changed")
	void underpaymentRejectedAndRollsBack() throws Exception {
		double stockBefore = stockOf(1);
		long id = billWithFriedRice(6);

		mvc.perform(post("/api/orders/" + id + "/pay")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod":"CASH","amountTendered":1.00}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("less than the total")));

		// The bill stays open, the stock untouched and the table still occupied.
		mvc.perform(get("/api/orders/" + id).header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.data.status").value("OPEN"));
		assertThat(stockOf(1)).isEqualTo(stockBefore);
		mvc.perform(get("/api/tables/6").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.data.status").value("OCCUPIED"));
	}

	@Test
	@DisplayName("cash without an amount tendered is rejected")
	void cashRequiresTender() throws Exception {
		long id = billWithFriedRice(7);

		mvc.perform(post("/api/orders/" + id + "/pay")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod":"CASH"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("required for a cash payment")));
	}

	/* ---- Non-cash --------------------------------------------------------- */

	@Test
	@DisplayName("card and QR settle the exact total with no change")
	void cardSettlesExactly() throws Exception {
		long id = billWithFriedRice(8);

		JsonNode paid = pay(id, """
				{"paymentMethod":"KHQR"}""");

		assertThat(paid.path("paymentMethod").asText()).isEqualTo("KHQR");
		assertThat(paid.path("amountTendered").asDouble()).isEqualTo(9.90);
		assertThat(paid.path("changeAmount").asDouble()).isZero();
	}

	/* ---- Guards ------------------------------------------------------------ */

	@Test
	@DisplayName("a bill cannot be paid twice")
	void cannotPayTwice() throws Exception {
		long id = billWithFriedRice(10);

		pay(id, """
				{"paymentMethod":"CASH","amountTendered":10.00}""");

		mvc.perform(post("/api/orders/" + id + "/pay")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod":"CASH","amountTendered":10.00}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("PAID")));
	}

	@Test
	@DisplayName("an empty bill cannot be paid")
	void cannotPayEmptyBill() throws Exception {
		MvcResult opened = mvc.perform(post("/api/orders")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"tableId":11,"guestCount":2}"""))
				.andReturn();
		long id = json.readTree(opened.getResponse().getContentAsString())
				.path("data").path("id").asLong();

		mvc.perform(post("/api/orders/" + id + "/pay")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod":"CASH","amountTendered":10.00}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("empty bill")));
	}

	@Test
	@DisplayName("a discount given at payment time is applied before VAT")
	void discountAtPayment() throws Exception {
		long id = billWithFriedRice(12);

		JsonNode paid = pay(id, """
				{"paymentMethod":"CASH","amountTendered":10.00,"discount":1.00}""");

		assertThat(paid.path("subtotal").asDouble()).isEqualTo(9.00);
		assertThat(paid.path("discount").asDouble()).isEqualTo(1.00);
		assertThat(paid.path("vatAmount").asDouble()).isEqualTo(0.80);
		assertThat(paid.path("total").asDouble()).isEqualTo(8.80);
		assertThat(paid.path("changeAmount").asDouble()).isEqualTo(1.20);
	}

	@Test
	@DisplayName("an unknown payment method is a 400 that names the accepted values")
	void unknownMethod() throws Exception {
		long id = billWithFriedRice(2);

		mvc.perform(post("/api/orders/" + id + "/pay")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod":"BITCOIN","amountTendered":10.00}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.allOf(
								org.hamcrest.Matchers.containsString("paymentMethod"),
								org.hamcrest.Matchers.containsString("KHQR"))));
	}

	@Test
	@DisplayName("malformed JSON is a 400, not a 500")
	void malformedJson() throws Exception {
		long id = billWithFriedRice(5);

		mvc.perform(post("/api/orders/" + id + "/pay")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"paymentMethod\":\"CASH\","))
				.andExpect(status().isBadRequest());
	}

	/* ---- Receipt ------------------------------------------------------------ */

	@Test
	@DisplayName("the receipt carries the restaurant header and the paid order")
	void receiptProjection() throws Exception {
		long id = billWithFriedRice(9);
		pay(id, """
				{"paymentMethod":"CASH","amountTendered":10.00}""");

		mvc.perform(get("/api/orders/" + id + "/receipt")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.restaurantNameEn").value("Angkor Restaurant"))
				.andExpect(jsonPath("$.data.phone").value("012 345 678"))
				.andExpect(jsonPath("$.data.order.status").value("PAID"))
				.andExpect(jsonPath("$.data.order.items.length()").value(1))
				.andExpect(jsonPath("$.data.order.totalKhr").value(40590));   // 9.90 × 4100
	}

	@Test
	@DisplayName("payment requires authentication")
	void requiresAuth() throws Exception {
		mvc.perform(post("/api/orders/1/pay")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod":"CASH","amountTendered":1}"""))
				.andExpect(status().isUnauthorized());
	}
}
