package com.resturant.management.rms.report;

import com.fasterxml.jackson.databind.JsonNode;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Milestone 12 — dashboards, the sales report and CSV export. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional   // roll back after each test: one H2 instance is shared by every test class
class ReportControllerTest {

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;

	private String admin;
	private String cashier;
	private final String today = LocalDate.now().toString();

	@BeforeEach
	void signIn() throws Exception {
		admin = token("admin");
		cashier = token("cashier");
	}

	private String token(String username) throws Exception {
		MvcResult res = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"%s\",\"password\":\"ChangeMe123!\"}".formatted(username)))
				.andReturn();
		return "Bearer " + json.readTree(res.getResponse().getContentAsString())
				.path("data").path("accessToken").asText();
	}

	private JsonNode data(MvcResult res) throws Exception {
		return json.readTree(res.getResponse().getContentAsString()).path("data");
	}

	/** Sells 2 × fried rice (price 4.50, cost 2.10) → revenue 9.90, cost 4.20. */
	private void sell(long tableId) throws Exception {
		MvcResult opened = mvc.perform(post("/api/orders").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tableId\":%d}".formatted(tableId)))
				.andReturn();
		long id = data(opened).path("id").asLong();

		mvc.perform(put("/api/orders/" + id + "/items").header("Authorization", cashier)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"items":[{"productId":1,"qty":2}]}"""));

		mvc.perform(post("/api/orders/" + id + "/pay").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod":"CASH","amountTendered":20}"""))
				.andExpect(status().isOk());
	}

	/* ---- Access ----------------------------------------------------------- */

	@Test
	@DisplayName("admin reports are ADMIN-only; the cashier home is not")
	void access() throws Exception {
		mvc.perform(get("/api/dashboard/summary").header("Authorization", admin))
				.andExpect(status().isOk());
		mvc.perform(get("/api/dashboard/summary").header("Authorization", cashier))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/reports/sales").header("Authorization", cashier))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/reports/sales.csv").header("Authorization", cashier))
				.andExpect(status().isForbidden());

		// A cashier needs their own figures.
		mvc.perform(get("/api/dashboard/cashier").header("Authorization", cashier))
				.andExpect(status().isOk());

		mvc.perform(get("/api/dashboard/summary")).andExpect(status().isUnauthorized());
	}

	/* ---- Dashboard --------------------------------------------------------- */

	@Test
	@DisplayName("dashboard reports today's sales, table status and low stock")
	void dashboard() throws Exception {
		sell(1);
		sell(3);

		JsonNode d = data(mvc.perform(get("/api/dashboard/summary")
				.header("Authorization", admin)).andReturn());

		assertThat(d.path("todaySales").asDouble()).isEqualTo(19.80);
		assertThat(d.path("monthSales").asDouble()).isEqualTo(19.80);
		assertThat(d.path("paidInvoices").asLong()).isEqualTo(2);
		assertThat(d.path("lowStockCount").asLong()).isEqualTo(5);
		assertThat(d.path("bestSellers").get(0).path("productName").asText())
				.isEqualTo("បាយឆាគ្រឿងសមុទ្រ");
		assertThat(d.path("lowStock").size()).isEqualTo(5);
	}

	@Test
	@DisplayName("the 7-day series zero-fills quiet days instead of omitting them")
	void sevenDaySeriesIsDense() throws Exception {
		sell(1);

		JsonNode series = data(mvc.perform(get("/api/dashboard/summary")
				.header("Authorization", admin)).andReturn()).path("lastSevenDays");

		assertThat(series.size()).isEqualTo(7);

		// Six quiet days at zero, today carrying the sale.
		long zeroDays = 0;
		for (JsonNode point : series) {
			if (point.path("total").asDouble() == 0.0) zeroDays++;
		}
		assertThat(zeroDays).isEqualTo(6);
		assertThat(series.get(6).path("date").asText()).isEqualTo(today);
		assertThat(series.get(6).path("total").asDouble()).isEqualTo(9.90);
		assertThat(series.get(6).path("orders").asLong()).isEqualTo(1);
	}

	@Test
	@DisplayName("cashier home reports today's takings and table usage")
	void cashierHome() throws Exception {
		sell(1);

		JsonNode d = data(mvc.perform(get("/api/dashboard/cashier")
				.header("Authorization", cashier)).andReturn());

		assertThat(d.path("todaySales").asDouble()).isEqualTo(9.90);
		assertThat(d.path("todayInvoices").asLong()).isEqualTo(1);
		assertThat(d.path("tablesTotal").asLong()).isEqualTo(12);
	}

	/* ---- Sales report ------------------------------------------------------ */

	@Test
	@DisplayName("sales report computes cost, gross profit and margin")
	void salesReport() throws Exception {
		sell(1);
		sell(3);

		JsonNode r = data(mvc.perform(get("/api/reports/sales?from=%s&to=%s".formatted(today, today))
				.header("Authorization", admin)).andReturn());

		// 2 bills × (2 × 4.50 = 9.00 + 10% VAT = 9.90) → revenue 19.80
		// cost 2 bills × (2 × 2.10) = 8.40 → profit 11.40, margin 57.58%
		assertThat(r.path("revenue").asDouble()).isEqualTo(19.80);
		assertThat(r.path("cost").asDouble()).isEqualTo(8.40);
		assertThat(r.path("grossProfit").asDouble()).isEqualTo(11.40);
		assertThat(r.path("marginPercent").asDouble()).isEqualTo(57.58);
		assertThat(r.path("invoiceCount").asLong()).isEqualTo(2);
		assertThat(r.path("averageSale").asDouble()).isEqualTo(9.90);
	}

	@Test
	@DisplayName("category breakdown attributes revenue and a percentage")
	void categoryBreakdown() throws Exception {
		sell(1);

		JsonNode byCategory = data(mvc.perform(
				get("/api/reports/sales?from=%s&to=%s".formatted(today, today))
						.header("Authorization", admin)).andReturn()).path("byCategory");

		assertThat(byCategory.size()).isEqualTo(1);
		assertThat(byCategory.get(0).path("nameEn").asText()).isEqualTo("Rice");
		assertThat(byCategory.get(0).path("qty").asDouble()).isEqualTo(2.0);
		assertThat(byCategory.get(0).path("revenue").asDouble()).isEqualTo(9.00);   // pre-VAT lines
	}

	@Test
	@DisplayName("category shares sum to 100%, not to a fraction of VAT-inclusive revenue")
	void categorySharesSumTo100() throws Exception {
		sell(1);   // Rice
		// A drink from a second category, so the breakdown has something to split.
		MvcResult opened = mvc.perform(post("/api/orders").header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"tableId":3}"""))
				.andReturn();
		long id = data(opened).path("id").asLong();
		mvc.perform(put("/api/orders/" + id + "/items").header("Authorization", cashier)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"items":[{"productId":10,"qty":4}]}"""));
		mvc.perform(post("/api/orders/" + id + "/pay").header("Authorization", cashier)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"paymentMethod":"CASH","amountTendered":20}"""));

		JsonNode byCategory = data(mvc.perform(
				get("/api/reports/sales?from=%s&to=%s".formatted(today, today))
						.header("Authorization", admin)).andReturn()).path("byCategory");

		assertThat(byCategory.size()).isEqualTo(2);

		double sum = 0;
		for (JsonNode c : byCategory) sum += c.path("percent").asDouble();
		assertThat(sum).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.2));
	}

	@Test
	@DisplayName("an empty range reports zeros rather than dividing by zero")
	void emptyRange() throws Exception {
		mvc.perform(get("/api/reports/sales?from=%s&to=%s".formatted(
								LocalDate.now().minusDays(60), LocalDate.now().minusDays(50)))
						.header("Authorization", admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.revenue").value(0))
				.andExpect(jsonPath("$.data.marginPercent").value(0))
				.andExpect(jsonPath("$.data.averageSale").value(0))
				.andExpect(jsonPath("$.data.byCategory.length()").value(0));
	}

	@Test
	@DisplayName("sales detail gives one row per paid invoice with its own profit")
	void salesDetail() throws Exception {
		sell(1);

		JsonNode rows = data(mvc.perform(
				get("/api/reports/sales/detail?from=%s&to=%s".formatted(today, today))
						.header("Authorization", admin)).andReturn());

		assertThat(rows.size()).isEqualTo(1);
		assertThat(rows.get(0).path("invoiceNo").asText()).startsWith("INV-");
		assertThat(rows.get(0).path("itemCount").asInt()).isEqualTo(1);
		assertThat(rows.get(0).path("total").asDouble()).isEqualTo(9.90);
		assertThat(rows.get(0).path("cost").asDouble()).isEqualTo(4.20);
		assertThat(rows.get(0).path("profit").asDouble()).isEqualTo(5.70);
		assertThat(rows.get(0).path("paymentMethod").asText()).isEqualTo("CASH");
	}

	/* ---- CSV ---------------------------------------------------------------- */

	@Test
	@DisplayName("CSV export returns a downloadable file, not the JSON envelope")
	void csvExport() throws Exception {
		sell(1);

		MvcResult res = mvc.perform(get("/api/reports/sales.csv?from=%s&to=%s".formatted(today, today))
						.header("Authorization", admin))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Disposition",
						org.hamcrest.Matchers.containsString("attachment; filename=")))
				.andReturn();

		String csv = res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

		assertThat(csv).doesNotContain("\"status\":200");        // not wrapped in ApiResponse
		assertThat(csv).contains("Date,Invoice,Table,Cashier,Items,Total,Cost,Profit,Payment");
		assertThat(csv).contains("INV-");
		assertThat(csv).contains("9.90");
		assertThat(csv).contains("CASH");
		assertThat(csv.lines().count()).isEqualTo(2);            // header + one row
	}

	@Test
	@DisplayName("CSV over an empty range still returns the header row")
	void csvEmptyRange() throws Exception {
		MvcResult res = mvc.perform(get("/api/reports/sales.csv?from=%s&to=%s".formatted(
								LocalDate.now().minusDays(60), LocalDate.now().minusDays(50)))
						.header("Authorization", admin))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
				.contains("Date,Invoice");
	}

	@Test
	@DisplayName("a bad date on a report is a 400, not a 500")
	void badDate() throws Exception {
		mvc.perform(get("/api/reports/sales?from=nonsense").header("Authorization", admin))
				.andExpect(status().isBadRequest());
	}
}
