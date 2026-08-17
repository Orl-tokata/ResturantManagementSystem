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

/** Milestone 8 — opening bills, editing the basket, and the money arithmetic. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional   // roll back after each test: one H2 instance is shared by every test class
class OrderControllerTest {

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

	private JsonNode openAt(long tableId) throws Exception {
		MvcResult res = mvc.perform(post("/api/orders")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tableId\":%d,\"guestCount\":4}".formatted(tableId)))
				.andExpect(status().isCreated())
				.andReturn();
		return json.readTree(res.getResponse().getContentAsString()).path("data");
	}

	/* ---- Opening ---------------------------------------------------------- */

	@Test
	@DisplayName("opening a bill numbers it from the sequence and occupies the table")
	void openOrder() throws Exception {
		JsonNode order = openAt(6);

		assertThat(order.path("invoiceNo").asText()).startsWith("INV-");
		assertThat(order.path("status").asText()).isEqualTo("OPEN");
		assertThat(order.path("tableName").asText()).isEqualTo("Table 06");
		assertThat(order.path("cashierName").asText()).isEqualTo("Sok Dara");

		mvc.perform(get("/api/tables/6").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.data.status").value("OCCUPIED"));
	}

	@Test
	@DisplayName("opening the same table twice reuses the bill instead of duplicating it")
	void openIsIdempotentPerTable() throws Exception {
		long first = openAt(8).path("id").asLong();
		long second = openAt(8).path("id").asLong();
		assertThat(second).isEqualTo(first);
	}

	@Test
	@DisplayName("invoice numbers are unique across bills")
	void invoiceNumbersAreUnique() throws Exception {
		String a = openAt(10).path("invoiceNo").asText();
		String b = openAt(12).path("invoiceNo").asText();
		assertThat(a).isNotEqualTo(b);
	}

	@Test
	@DisplayName("opening at a table that does not exist is a 404")
	void unknownTable() throws Exception {
		mvc.perform(post("/api/orders")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"tableId":9999,"guestCount":2}"""))
				.andExpect(status().isNotFound());
	}

	/* ---- Basket and totals ------------------------------------------------- */

	@Test
	@DisplayName("totals: subtotal, 10% VAT, riel conversion")
	void totalsAreComputed() throws Exception {
		long id = openAt(1).path("id").asLong();

		// 2 × 4.50 (fried rice) + 4 × 1.50 (beer) = 15.00
		MvcResult res = mvc.perform(put("/api/orders/" + id + "/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items":[{"productId":1,"qty":2},{"productId":10,"qty":4}]}"""))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode data = json.readTree(res.getResponse().getContentAsString()).path("data");

		assertThat(data.path("subtotal").asDouble()).isEqualTo(15.00);
		assertThat(data.path("vatRate").asDouble()).isEqualTo(10.0);
		assertThat(data.path("vatAmount").asDouble()).isEqualTo(1.50);
		assertThat(data.path("total").asDouble()).isEqualTo(16.50);
		assertThat(data.path("totalKhr").asDouble()).isEqualTo(67650);   // 16.50 × 4100
		assertThat(data.path("items")).hasSize(2);
	}

	@Test
	@DisplayName("a discount is applied before VAT")
	void discountAppliedBeforeVat() throws Exception {
		long id = openAt(3).path("id").asLong();

		MvcResult res = mvc.perform(put("/api/orders/" + id + "/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items":[{"productId":1,"qty":2}],"discount":1.00}"""))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode data = json.readTree(res.getResponse().getContentAsString()).path("data");
		assertThat(data.path("subtotal").asDouble()).isEqualTo(9.00);
		assertThat(data.path("vatAmount").asDouble()).isEqualTo(0.80);   // 10% of 8.00
		assertThat(data.path("total").asDouble()).isEqualTo(8.80);
	}

	@Test
	@DisplayName("a discount larger than the subtotal is rejected")
	void discountCannotExceedSubtotal() throws Exception {
		long id = openAt(11).path("id").asLong();

		mvc.perform(put("/api/orders/" + id + "/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items":[{"productId":12,"qty":1}],"discount":99.00}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("Discount cannot exceed")));
	}

	@Test
	@DisplayName("replacing items swaps the whole basket rather than appending")
	void replaceReplaces() throws Exception {
		long id = openAt(4).path("id").asLong();

		mvc.perform(put("/api/orders/" + id + "/items")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"items":[{"productId":1,"qty":1},{"productId":2,"qty":1}]}"""))
				.andExpect(jsonPath("$.data.items.length()").value(2));

		mvc.perform(put("/api/orders/" + id + "/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items":[{"productId":5,"qty":3}]}"""))
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].productName").value("មាន់អាំងឃ្មុំ"))
				.andExpect(jsonPath("$.data.subtotal").value(22.50));
	}

	@Test
	@DisplayName("line items freeze the product name and unit price")
	void itemsSnapshotProduct() throws Exception {
		long id = openAt(5).path("id").asLong();

		mvc.perform(put("/api/orders/" + id + "/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items":[{"productId":2,"qty":2,"note":"មិនដាក់ស្ករ"}]}"""))
				.andExpect(jsonPath("$.data.items[0].productName").value("គុយទាវសាច់គោ"))
				.andExpect(jsonPath("$.data.items[0].unitPrice").value(3.50))
				.andExpect(jsonPath("$.data.items[0].lineTotal").value(7.00))
				.andExpect(jsonPath("$.data.items[0].note").value("មិនដាក់ស្ករ"));
	}

	@Test
	@DisplayName("zero quantity is rejected")
	void rejectsZeroQty() throws Exception {
		long id = openAt(7).path("id").asLong();

		mvc.perform(put("/api/orders/" + id + "/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items":[{"productId":1,"qty":0}]}"""))
				.andExpect(status().isBadRequest());
	}

	/* ---- Cancel ------------------------------------------------------------ */

	@Test
	@DisplayName("cancelling frees the table and locks the bill")
	void cancelFreesTable() throws Exception {
		long id = openAt(9).path("id").asLong();

		mvc.perform(post("/api/orders/" + id + "/cancel")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("CANCELLED"));

		mvc.perform(get("/api/tables/9").header("Authorization", "Bearer " + token))
				.andExpect(jsonPath("$.data.status").value("FREE"));

		// A cancelled bill can no longer be edited.
		mvc.perform(put("/api/orders/" + id + "/items")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"items":[{"productId":1,"qty":1}]}"""))
				.andExpect(status().isBadRequest());
	}

	/* ---- Lookup ------------------------------------------------------------ */

	@Test
	@DisplayName("the open bill at a table can be found again after a reload")
	void findOpenOrderForTable() throws Exception {
		long id = openAt(2).path("id").asLong();

		mvc.perform(get("/api/orders/open?tableId=2").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(id));
	}

	@Test
	@DisplayName("a table with no open bill returns 404, not an empty object")
	void noOpenOrder() throws Exception {
		mvc.perform(get("/api/orders/open?tableId=12").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("orders require authentication")
	void requiresAuth() throws Exception {
		mvc.perform(get("/api/orders/1")).andExpect(status().isUnauthorized());
	}
}
