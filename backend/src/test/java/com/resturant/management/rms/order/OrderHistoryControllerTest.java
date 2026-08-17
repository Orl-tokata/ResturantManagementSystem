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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Milestone 10 — history listing, filters and the summary tiles. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional   // roll back after each test: one H2 instance is shared by every test class
class OrderHistoryControllerTest {

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;

	private String token;
	private final String today = LocalDate.now().toString();

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

	private String auth() {
		return "Bearer " + token;
	}

	/** Opens a bill at the table with 2 × fried rice (total 9.90). */
	private long bill(long tableId) throws Exception {
		MvcResult opened = mvc.perform(post("/api/orders")
						.header("Authorization", auth())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tableId\":%d}".formatted(tableId)))
				.andReturn();
		long id = json.readTree(opened.getResponse().getContentAsString())
				.path("data").path("id").asLong();

		mvc.perform(put("/api/orders/" + id + "/items")
				.header("Authorization", auth())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"items":[{"productId":1,"qty":2}]}"""));
		return id;
	}

	private void pay(long id) throws Exception {
		mvc.perform(post("/api/orders/" + id + "/pay")
						.header("Authorization", auth())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod":"CASH","amountTendered":20.00}"""))
				.andExpect(status().isOk());
	}

	/* ---- Listing ---------------------------------------------------------- */

	@Test
	@DisplayName("history starts empty and lists bills as they are created")
	void listsOrders() throws Exception {
		mvc.perform(get("/api/orders").header("Authorization", auth()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(0));

		bill(1);
		bill(3);

		mvc.perform(get("/api/orders").header("Authorization", auth()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(2));
	}

	@Test
	@DisplayName("filters by status")
	void filtersByStatus() throws Exception {
		long paid = bill(1);
		pay(paid);

		long cancelled = bill(3);
		mvc.perform(post("/api/orders/" + cancelled + "/cancel").header("Authorization", auth()));

		bill(4);   // stays OPEN

		mvc.perform(get("/api/orders?status=PAID").header("Authorization", auth()))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].status").value("PAID"));

		mvc.perform(get("/api/orders?status=CANCELLED").header("Authorization", auth()))
				.andExpect(jsonPath("$.data.totalElements").value(1));

		mvc.perform(get("/api/orders?status=OPEN").header("Authorization", auth()))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	@DisplayName("searches by invoice number")
	void searchesByInvoiceNo() throws Exception {
		long id = bill(1);
		MvcResult res = mvc.perform(get("/api/orders/" + id).header("Authorization", auth()))
				.andReturn();
		String invoiceNo = json.readTree(res.getResponse().getContentAsString())
				.path("data").path("invoiceNo").asText();

		mvc.perform(get("/api/orders?search=" + invoiceNo).header("Authorization", auth()))
				.andExpect(jsonPath("$.data.totalElements").value(1));

		mvc.perform(get("/api/orders?search=INV-99999").header("Authorization", auth()))
				.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	/* ---- Date range ------------------------------------------------------- */

	@Test
	@DisplayName("today's range includes a bill taken today")
	void dateRangeIncludesToday() throws Exception {
		bill(1);

		mvc.perform(get("/api/orders?from=%s&to=%s".formatted(today, today))
						.header("Authorization", auth()))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	@DisplayName("a range ending today includes bills taken later in the day, not just at midnight")
	void toDateIsInclusive() throws Exception {
		bill(1);

		// The regression this guards: an exclusive `to` at 00:00 would return 0.
		mvc.perform(get("/api/orders?from=%s&to=%s".formatted(
								LocalDate.now().minusDays(7), today))
						.header("Authorization", auth()))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}

	@Test
	@DisplayName("a past range excludes today's bills")
	void pastRangeExcludesToday() throws Exception {
		bill(1);

		mvc.perform(get("/api/orders?from=%s&to=%s".formatted(
								LocalDate.now().minusDays(30), LocalDate.now().minusDays(10)))
						.header("Authorization", auth()))
				.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	@DisplayName("an unparseable date is a 400, not a 500")
	void badDate() throws Exception {
		mvc.perform(get("/api/orders?from=not-a-date").header("Authorization", auth()))
				.andExpect(status().isBadRequest());
	}

	/* ---- Summary ---------------------------------------------------------- */

	@Test
	@DisplayName("summary totals only paid bills, and averages over them")
	void summaryCountsPaidOnly() throws Exception {
		pay(bill(1));
		pay(bill(3));

		long cancelled = bill(4);
		mvc.perform(post("/api/orders/" + cancelled + "/cancel").header("Authorization", auth()));

		bill(6);   // OPEN — counted in totalCount but not in sales

		MvcResult res = mvc.perform(get("/api/orders/summary?from=%s&to=%s".formatted(today, today))
						.header("Authorization", auth()))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode s = json.readTree(res.getResponse().getContentAsString()).path("data");

		assertThat(s.path("paidCount").asLong()).isEqualTo(2);
		assertThat(s.path("totalSales").asDouble()).isEqualTo(19.80);      // 2 × 9.90
		assertThat(s.path("averageSale").asDouble()).isEqualTo(9.90);
		assertThat(s.path("cancelledCount").asLong()).isEqualTo(1);
		assertThat(s.path("totalCount").asLong()).isEqualTo(4);
	}

	@Test
	@DisplayName("an empty range reports zeros rather than dividing by zero")
	void emptySummary() throws Exception {
		mvc.perform(get("/api/orders/summary?from=%s&to=%s".formatted(
								LocalDate.now().minusDays(60), LocalDate.now().minusDays(50)))
						.header("Authorization", auth()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.paidCount").value(0))
				.andExpect(jsonPath("$.data.totalSales").value(0))
				.andExpect(jsonPath("$.data.averageSale").value(0));
	}

	@Test
	@DisplayName("summary with no dates covers everything")
	void summaryWithoutDates() throws Exception {
		pay(bill(1));

		mvc.perform(get("/api/orders/summary").header("Authorization", auth()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.paidCount").value(1));
	}

	/* ---- Access ----------------------------------------------------------- */

	@Test
	@DisplayName("history requires authentication")
	void requiresAuth() throws Exception {
		mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/orders/summary")).andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("/summary is not swallowed by the /{id} route")
	void summaryPathWins() throws Exception {
		mvc.perform(get("/api/orders/summary").header("Authorization", auth()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalCount").exists());
	}
}
