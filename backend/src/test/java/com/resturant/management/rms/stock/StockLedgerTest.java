package com.resturant.management.rms.stock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resturant.management.rms.catalog.Product;
import com.resturant.management.rms.catalog.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Every change to a stock figure leaves a movement, and the movements explain
 * the figure.
 *
 * <p>Not {@code @Transactional}: a sale and the stock it moves commit together,
 * and a rolled-back test would assert against numbers the server never kept.
 * What it creates is cancelled or restored in the test itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StockLedgerTest {

	/** A seeded FREE table and a product with stock on hand. */
	private static final long TABLE = 6;
	private static final long PRODUCT = 1;

	@Autowired MockMvc mvc;
	@Autowired ObjectMapper json;
	@Autowired ProductRepository products;
	@Autowired StockMovementRepository movements;

	private String cashier;
	private String admin;

	@BeforeEach
	void signIn() throws Exception {
		cashier = "Bearer " + token("cashier");
		admin = "Bearer " + token("admin");
	}

	private String token(String username) throws Exception {
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

	private BigDecimal stockOf(long productId) {
		return products.findById(productId).orElseThrow().getStockQty();
	}

	/** Opens a bill, puts one product on it, pays, and returns the order id. */
	private long sell(long productId, int qty) throws Exception {
		MvcResult opened = mvc.perform(post("/api/orders")
						.header("Authorization", cashier)
						.header("Idempotency-Key", key())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"tableId\":%d,\"guestCount\":1}".formatted(TABLE)))
				.andExpect(status().isCreated())
				.andReturn();
		long id = json.readTree(opened.getResponse().getContentAsString())
				.path("data").path("id").asLong();

		mvc.perform(put("/api/orders/" + id + "/items")
						.header("Authorization", cashier)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"items\":[{\"productId\":%d,\"qty\":%d}]}".formatted(productId, qty)))
				.andExpect(status().isOk());

		mvc.perform(post("/api/orders/" + id + "/pay")
						.header("Authorization", cashier)
						.header("Idempotency-Key", key())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"paymentMethod":"CASH","amountTendered":500}"""))
				.andExpect(status().isOk());
		return id;
	}

	/* ---- The question the ledger exists to answer --------------------------- */

	/**
	 * Before V11 a sale subtracted from the figure and wrote nothing, so a count
	 * that looked wrong could not be traced to anything at all.
	 */
	@Test
	@DisplayName("a sale leaves a movement naming the bill that caused it")
	void saleIsRecorded() throws Exception {
		BigDecimal before = stockOf(PRODUCT);
		long orderId = sell(PRODUCT, 2);

		List<StockMovement> after = movements.findAll().stream()
				.filter(m -> m.getProduct() != null && m.getProduct().getId() == PRODUCT)
				.filter(m -> m.getMovementType() == MovementType.SALE)
				.filter(m -> orderId == (m.getRefId() == null ? -1L : m.getRefId()))
				.toList();

		assertThat(after).as("one movement for the one line sold").hasSize(1);
		StockMovement m = after.get(0);
		assertThat(m.getQty()).isEqualByComparingTo("2");
		assertThat(m.getRefType()).isEqualTo(StockLedger.REF_ORDER);
		assertThat(m.getBalanceAfter())
				.as("the figure the screen showed after this sale")
				.isEqualByComparingTo(before.subtract(new BigDecimal("2")));
	}

	/**
	 * The reconciliation docs/PLAN.md P4 asks for: the stored figure and the
	 * ledger must agree, or every count is suspect from the first day.
	 */
	@Test
	@DisplayName("the stored quantity equals the balance of the newest movement")
	void storedQuantityMatchesTheLedger() throws Exception {
		sell(PRODUCT, 1);

		StockMovement newest = movements.findAll().stream()
				.filter(m -> m.getProduct() != null && m.getProduct().getId() == PRODUCT)
				.filter(m -> m.getBalanceAfter() != null)
				.max((a, b) -> {
					int byTime = a.getCreatedAt().compareTo(b.getCreatedAt());
					return byTime != 0 ? byTime : Long.compare(a.getId(), b.getId());
				})
				.orElseThrow();

		assertThat(newest.getBalanceAfter()).isEqualByComparingTo(stockOf(PRODUCT));
	}

	@Test
	@DisplayName("two of something moves two, not one")
	void quantityIsTheLineQuantity() throws Exception {
		BigDecimal before = stockOf(PRODUCT);
		sell(PRODUCT, 3);
		assertThat(stockOf(PRODUCT)).isEqualByComparingTo(before.subtract(new BigDecimal("3")));
	}

	/* ---- Reading it ---------------------------------------------------------- */

	@Test
	@DisplayName("a product's movements come back with their balances")
	void productMovementsEndpoint() throws Exception {
		sell(PRODUCT, 1);

		MvcResult res = mvc.perform(get("/api/stock/products/" + PRODUCT + "/movements")
						.header("Authorization", admin))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode rows = json.readTree(res.getResponse().getContentAsString())
				.path("data").path("content");
		assertThat(rows).isNotEmpty();

		JsonNode newest = rows.get(0);
		assertThat(newest.path("type").asText()).isEqualTo("SALE");
		assertThat(newest.path("increase").asBoolean()).isFalse();
		assertThat(newest.path("productId").asLong()).isEqualTo(PRODUCT);
		assertThat(newest.path("balanceAfter").isMissingNode()).isFalse();
		assertThat(newest.path("refType").asText()).isEqualTo("ORDER");
	}

	@Test
	@DisplayName("the whole ledger carries ingredients and products together")
	void ledgerEndpoint() throws Exception {
		MvcResult res = mvc.perform(get("/api/stock/ledger?size=100").header("Authorization", admin))
				.andExpect(status().isOk())
				.andReturn();

		JsonNode rows = json.readTree(res.getResponse().getContentAsString())
				.path("data").path("content");

		boolean anyProduct = false;
		boolean anyItem = false;
		for (JsonNode r : rows) {
			boolean hasProduct = !r.path("productId").isNull() && r.path("productId").asLong(0) > 0;
			boolean hasItem = !r.path("stockItemId").isNull() && r.path("stockItemId").asLong(0) > 0;
			// The table's own constraint, seen from the outside.
			assertThat(hasProduct ^ hasItem)
					.as("a movement is about one thing, never both or neither")
					.isTrue();
			anyProduct |= hasProduct;
			anyItem |= hasItem;
		}
		assertThat(anyProduct).as("opening balances exist for products").isTrue();
		assertThat(anyItem).as("and for ingredients").isTrue();
	}

	/* ---- Corrections ---------------------------------------------------------- */

	@Test
	@DisplayName("an adjustment that would go below empty is refused")
	void adjustmentCannotGoNegative() throws Exception {
		mvc.perform(post("/api/stock/1/adjust")
						.header("Authorization", admin)
						.header("Idempotency-Key", key())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"type":"OUT","qty":99999,"reason":"more than exists"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("error.stock.insufficient"));
	}

	/**
	 * A sale is recorded after the food has left the kitchen, so refusing it
	 * would record a lie to keep a number tidy.
	 */
	@Test
	@DisplayName("a sale may take stock negative, and still leaves a movement")
	void saleMayGoNegative() throws Exception {
		Product p = products.findById(PRODUCT).orElseThrow();
		BigDecimal restore = p.getStockQty();
		try {
			p.setStockQty(BigDecimal.ONE);
			products.save(p);

			sell(PRODUCT, 5);

			assertThat(stockOf(PRODUCT)).isEqualByComparingTo("-4");
			assertThat(movements.findAll().stream()
					.anyMatch(m -> m.getProduct() != null
							&& m.getProduct().getId() == PRODUCT
							&& m.getBalanceAfter() != null
							&& m.getBalanceAfter().compareTo(BigDecimal.ZERO) < 0))
					.as("the movement that took it negative is on the record")
					.isTrue();
		} finally {
			Product again = products.findById(PRODUCT).orElseThrow();
			again.setStockQty(restore);
			products.save(again);
		}
	}
}
