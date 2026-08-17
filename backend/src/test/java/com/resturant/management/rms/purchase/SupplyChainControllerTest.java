package com.resturant.management.rms.purchase;

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

/** Milestone 11 — suppliers, purchase orders, goods receipt and stock adjustments. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional   // roll back after each test: one H2 instance is shared by every test class
class SupplyChainControllerTest {

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;

	private String admin;
	private String cashier;

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

	private double stockQty(long id) throws Exception {
		return data(mvc.perform(get("/api/stock/" + id).header("Authorization", admin)).andReturn())
				.path("qty").asDouble();
	}

	/* ---- Access ----------------------------------------------------------- */

	@Test
	@DisplayName("the whole supply chain is ADMIN-only")
	void adminOnly() throws Exception {
		for (String path : new String[] { "/api/suppliers", "/api/stock", "/api/purchases" }) {
			mvc.perform(get(path).header("Authorization", admin)).andExpect(status().isOk());
			mvc.perform(get(path).header("Authorization", cashier)).andExpect(status().isForbidden());
			mvc.perform(get(path)).andExpect(status().isUnauthorized());
		}
	}

	/* ---- Suppliers -------------------------------------------------------- */

	@Test
	@DisplayName("seeded suppliers and stock items are listed")
	void listsSeedData() throws Exception {
		mvc.perform(get("/api/suppliers").header("Authorization", admin))
				.andExpect(jsonPath("$.data.totalElements").value(5));
		mvc.perform(get("/api/stock").header("Authorization", admin))
				.andExpect(jsonPath("$.data.totalElements").value(10));
		mvc.perform(get("/api/suppliers/active").header("Authorization", admin))
				.andExpect(jsonPath("$.data.length()").value(4));   // 5 seeded, 1 INACTIVE
	}

	@Test
	@DisplayName("duplicate supplier code is rejected")
	void duplicateSupplierCode() throws Exception {
		mvc.perform(post("/api/suppliers").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"supplierCode":"SUP-001","company":"Copycat Ltd"}"""))
				.andExpect(status().isConflict());
	}

	@Test
	@DisplayName("a supplier with an outstanding balance cannot be deleted")
	void cannotDeleteSupplierWithBalance() throws Exception {
		// SUP-001 is seeded with a 1240.00 balance.
		mvc.perform(delete("/api/suppliers/1").header("Authorization", admin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("still outstanding")));
	}

	@Test
	@DisplayName("the balance cannot be set through the update endpoint")
	void balanceIsNotSettable() throws Exception {
		MvcResult res = mvc.perform(put("/api/suppliers/5").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"supplierCode":"SUP-005","company":"Rice Land Cambodia","balance":99999}"""))
				.andExpect(status().isOk())
				.andReturn();
		assertThat(data(res).path("balance").asDouble()).isZero();
	}

	/* ---- Stock adjustments ------------------------------------------------- */

	@Test
	@DisplayName("an IN adjustment adds stock and records a movement")
	void adjustIn() throws Exception {
		double before = stockQty(2);   // beef, seeded at 2.5

		MvcResult res = mvc.perform(post("/api/stock/2/adjust").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"type":"IN","qty":10,"reason":"ទិញបន្ថែម"}"""))
				.andExpect(status().isOk())
				.andReturn();

		assertThat(data(res).path("qty").asDouble()).isEqualTo(before + 10);

		mvc.perform(get("/api/stock/2/movements").header("Authorization", admin))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].type").value("IN"))
				.andExpect(jsonPath("$.data.content[0].createdBy").value("admin"))
				.andExpect(jsonPath("$.data.content[0].reason").value("ទិញបន្ថែម"));
	}

	@Test
	@DisplayName("OUT and DAMAGED remove stock even though qty is sent positive")
	void adjustOutAndDamaged() throws Exception {
		mvc.perform(post("/api/stock/1/adjust").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"type":"OUT","qty":20}"""))
				.andExpect(jsonPath("$.data.qty").value(100.0));      // 120 − 20

		mvc.perform(post("/api/stock/1/adjust").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"type":"DAMAGED","qty":5,"reason":"ខូច"}"""))
				.andExpect(jsonPath("$.data.qty").value(95.0));
	}

	@Test
	@DisplayName("an adjustment cannot drive stock negative")
	void adjustCannotGoNegative() throws Exception {
		double before = stockQty(2);

		mvc.perform(post("/api/stock/2/adjust").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"type":"OUT","qty":999}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("only")));

		assertThat(stockQty(2)).isEqualTo(before);
	}

	@Test
	@DisplayName("a zero or negative adjustment quantity is rejected")
	void adjustRejectsNonPositive() throws Exception {
		mvc.perform(post("/api/stock/1/adjust").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"type":"IN","qty":0}"""))
				.andExpect(status().isBadRequest());

		mvc.perform(post("/api/stock/1/adjust").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"type":"IN","qty":-5}"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("stock summary reflects the seeded shortfalls")
	void stockSummary() throws Exception {
		mvc.perform(get("/api/stock/summary").header("Authorization", admin))
				.andExpect(jsonPath("$.data.totalItems").value(10))
				.andExpect(jsonPath("$.data.lowStockCount").value(5))
				.andExpect(jsonPath("$.data.outOfStockCount").value(1));

		mvc.perform(get("/api/stock/low").header("Authorization", admin))
				.andExpect(jsonPath("$.data.length()").value(5));
	}

	@Test
	@DisplayName("an item with movement history cannot be deleted")
	void cannotDeleteItemWithHistory() throws Exception {
		mvc.perform(post("/api/stock/3/adjust").header("Authorization", admin)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"type":"IN","qty":1}"""));

		mvc.perform(delete("/api/stock/3").header("Authorization", admin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("movement history")));
	}

	/* ---- Purchase orders ---------------------------------------------------- */

	private long raisePo() throws Exception {
		MvcResult res = mvc.perform(post("/api/purchases").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"supplierId":1,"purchaseDate":"%s","items":[
								  {"stockItemId":2,"qty":20,"unitCost":8.50},
								  {"stockItemId":4,"qty":15,"unitCost":12.00}
								]}""".formatted(LocalDate.now())))
				.andExpect(status().isCreated())
				.andReturn();
		return data(res).path("id").asLong();
	}

	@Test
	@DisplayName("raising an order totals the lines and leaves stock untouched")
	void raisePurchaseOrder() throws Exception {
		double beefBefore = stockQty(2);

		MvcResult res = mvc.perform(post("/api/purchases").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"supplierId":1,"purchaseDate":"%s","items":[
								  {"stockItemId":2,"qty":20,"unitCost":8.50}
								]}""".formatted(LocalDate.now())))
				.andExpect(status().isCreated())
				.andReturn();

		JsonNode po = data(res);
		assertThat(po.path("poNo").asText()).startsWith("PO-");
		assertThat(po.path("status").asText()).isEqualTo("PENDING");
		assertThat(po.path("total").asDouble()).isEqualTo(170.00);
		assertThat(po.path("items").get(0).path("itemName").asText()).isEqualTo("សាច់គោ");

		assertThat(stockQty(2)).isEqualTo(beefBefore);   // nothing received yet
	}

	@Test
	@DisplayName("receiving adds stock, writes movements, refreshes cost and bills the supplier")
	void receiveGoods() throws Exception {
		double beefBefore = stockQty(2);
		double prawnBefore = stockQty(4);

		double payableBefore = data(mvc.perform(get("/api/suppliers/1")
				.header("Authorization", admin)).andReturn()).path("balance").asDouble();

		long id = raisePo();

		mvc.perform(post("/api/purchases/" + id + "/receive").header("Authorization", admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("RECEIVED"));

		assertThat(stockQty(2)).isEqualTo(beefBefore + 20);
		assertThat(stockQty(4)).isEqualTo(prawnBefore + 15);

		// A movement per line, attributed to whoever received it.
		mvc.perform(get("/api/stock/2/movements").header("Authorization", admin))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].type").value("IN"))
				.andExpect(jsonPath("$.data.content[0].reason")
						.value(org.hamcrest.Matchers.containsString("Received on PO-")));

		// 20 × 8.50 + 15 × 12.00 = 350.00 added to the payable.
		double payableAfter = data(mvc.perform(get("/api/suppliers/1")
				.header("Authorization", admin)).andReturn()).path("balance").asDouble();
		assertThat(payableAfter).isEqualTo(payableBefore + 350.00);
	}

	@Test
	@DisplayName("an order cannot be received twice")
	void cannotReceiveTwice() throws Exception {
		long id = raisePo();
		mvc.perform(post("/api/purchases/" + id + "/receive").header("Authorization", admin))
				.andExpect(status().isOk());

		mvc.perform(post("/api/purchases/" + id + "/receive").header("Authorization", admin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("already been received")));
	}

	@Test
	@DisplayName("a received order cannot be cancelled or deleted")
	void receivedOrderIsLocked() throws Exception {
		long id = raisePo();
		mvc.perform(post("/api/purchases/" + id + "/receive").header("Authorization", admin));

		mvc.perform(post("/api/purchases/" + id + "/cancel").header("Authorization", admin))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message")
						.value(org.hamcrest.Matchers.containsString("overstated")));

		mvc.perform(delete("/api/purchases/" + id).header("Authorization", admin))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("a cancelled order cannot then be received")
	void cancelledOrderCannotBeReceived() throws Exception {
		long id = raisePo();
		mvc.perform(post("/api/purchases/" + id + "/cancel").header("Authorization", admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("CANCELLED"));

		mvc.perform(post("/api/purchases/" + id + "/receive").header("Authorization", admin))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("an order with no lines is rejected")
	void rejectsEmptyOrder() throws Exception {
		mvc.perform(post("/api/purchases").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"supplierId":1,"purchaseDate":"%s","items":[]}""".formatted(LocalDate.now())))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("an order against a missing stock item is a 404")
	void rejectsUnknownStockItem() throws Exception {
		mvc.perform(post("/api/purchases").header("Authorization", admin)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"supplierId":1,"purchaseDate":"%s","items":[
								  {"stockItemId":9999,"qty":1,"unitCost":1}
								]}""".formatted(LocalDate.now())))
				.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("purchase summary reports pending count and total payable")
	void purchaseSummary() throws Exception {
		raisePo();

		mvc.perform(get("/api/purchases/summary").header("Authorization", admin))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.pendingCount").value(1))
				.andExpect(jsonPath("$.data.orderCount").value(1))
				.andExpect(jsonPath("$.data.monthTotal").value(350.00))
				.andExpect(jsonPath("$.data.payable").value(6930.00));   // seeded balances
	}

	@Test
	@DisplayName("purchase list filters by status")
	void filtersByStatus() throws Exception {
		long pending = raisePo();
		long cancelled = raisePo();
		mvc.perform(post("/api/purchases/" + cancelled + "/cancel").header("Authorization", admin));

		mvc.perform(get("/api/purchases?status=PENDING").header("Authorization", admin))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.content[0].id").value(pending));

		mvc.perform(get("/api/purchases?status=CANCELLED").header("Authorization", admin))
				.andExpect(jsonPath("$.data.totalElements").value(1));
	}
}
