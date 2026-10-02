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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Figures that must not move after the fact.
 *
 * <p>Two numbers on a settled bill were being read from live settings long
 * after it was settled: the exchange rate behind total_khr, and the cost
 * behind every margin. Correcting either rewrote history — the riel column of
 * a month already reported, and the profit of a year already closed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MoneyHistoryTest {

    /** Fried rice: 4.50, costing 2.10 (V3 seed). */
    private static final long PRODUCT = 1;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String admin;
    private String cashier;
    private final String today = LocalDate.now().toString();

    @BeforeEach
    void signIn() throws Exception {
        admin = token("admin");
        cashier = token("cashier");
        openShift(cashier);
    }

    /**
     * The P3 gate: a bill cannot be settled unless the cashier has a drawer
     * open. A shift already open answers 400, which is the right answer and
     * nothing here needs a second one.
     */
    private void openShift(String bearer) throws Exception {
        mvc.perform(post("/api/shifts")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"openingFloat":200.00}"""));
    }


    private String token(String username) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"ChangeMe123!\"}".formatted(username)))
                .andReturn();
        return "Bearer " + json.readTree(res.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    /** Sells {@code qty} of the seeded dish at the given table and settles in cash. */
    private JsonNode sell(long tableId, int qty) throws Exception {
        MvcResult opened = mvc.perform(post("/api/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableId\":%d,\"guestCount\":2}".formatted(tableId)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = json.readTree(opened.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mvc.perform(put("/api/orders/" + id + "/items")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":%d,\"qty\":%d}]}".formatted(PRODUCT, qty)))
                .andExpect(status().isOk());

        MvcResult paid = mvc.perform(post("/api/orders/" + id + "/pay")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"CASH","amountTendered":500}"""))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(paid.getResponse().getContentAsString()).path("data");
    }

    private JsonNode salesReport() throws Exception {
        MvcResult res = mvc.perform(get("/api/reports/sales?from=%s&to=%s".formatted(today, today))
                .header("Authorization", admin)).andReturn();
        return json.readTree(res.getResponse().getContentAsString()).path("data");
    }

    private void setProductCost(String cost) throws Exception {
        MvcResult current = mvc.perform(get("/api/products/" + PRODUCT)
                .header("Authorization", admin)).andReturn();
        JsonNode p = json.readTree(current.getResponse().getContentAsString()).path("data");

        mvc.perform(put("/api/products/" + PRODUCT)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","nameEn":"%s","categoryId":%d,"price":%s,"cost":%s,"status":"ACTIVE"}"""
                                .formatted(p.path("name").asText(), p.path("nameEn").asText(),
                                        p.path("categoryId").asLong(), p.path("price").asText(), cost)))
                .andExpect(status().isOk());
    }

    /* ---- Cost ------------------------------------------------------------- */

    /**
     * The bug this package exists to fix, stated as a test: sell something,
     * then change what it costs. Before V13 the sale's margin changed with it,
     * because the report multiplied last week's quantities by today's cost.
     */
    @Test
    @DisplayName("repricing a dish does not change the margin on sales already made")
    void repricingDoesNotRewriteHistory() throws Exception {
        java.math.BigDecimal before = salesReport().path("cost").decimalValue();

        sell(1, 2);
        java.math.BigDecimal afterSale = salesReport().path("cost").decimalValue();
        assertThat(afterSale.subtract(before))
                .as("two dishes at the seeded cost of 2.10")
                .isEqualByComparingTo("4.20");

        setProductCost("9.99");

        assertThat(salesReport().path("cost").decimalValue())
                .as("the sale already happened, at the cost it happened at")
                .isEqualByComparingTo(afterSale);
    }

    @Test
    @DisplayName("a line sold now records its own cost and is not an estimate")
    void soldNowIsRecordedNotEstimated() throws Exception {
        sell(3, 1);

        JsonNode rows = salesReport();
        assertThat(rows.path("estimatedCostLines").asLong())
                .as("nothing in this range predates the ledger of costs")
                .isZero();

        MvcResult res = mvc.perform(get("/api/reports/sales/detail?from=%s&to=%s".formatted(today, today))
                .header("Authorization", admin)).andReturn();
        JsonNode detail = json.readTree(res.getResponse().getContentAsString()).path("data");
        assertThat(detail).isNotEmpty();
        assertThat(detail.get(0).path("costEstimated").asBoolean()).isFalse();
    }

    /* ---- Rate -------------------------------------------------------------- */

    @Test
    @DisplayName("a bill records the rate it was converted at")
    void billStampsItsRate() throws Exception {
        JsonNode paid = sell(5, 1);

        assertThat(paid.path("totalKhr").asLong()).isEqualTo(20295);   // 4.95 x 4100
        assertThat(paid.path("fxRateKhr").decimalValue())
                .as("not divided out of the total afterwards, recorded")
                .isEqualByComparingTo("4100");
    }

    /**
     * valid_from is a date, so the table holds one rate per day and the last
     * save of a day is the one the tills used for most of it. What a single
     * bill was converted at does not live here at all — it is stamped on the
     * bill, which is why correcting a typo cannot disturb a printed receipt.
     */
    @Test
    @DisplayName("the day's rate is corrected in place, and saving the same number changes nothing")
    void rateChangesAreHistory() throws Exception {
        // V13 opened the history with today's rate, the way V11 opened every
        // product's with its current quantity.
        assertThat(fxRates()).hasSize(1);
        assertThat(fxRates().get(0).path("rate").decimalValue()).isEqualByComparingTo("4100");

        saveRate("4150");
        assertThat(fxRates()).hasSize(1);
        assertThat(fxRates().get(0).path("rate").decimalValue()).isEqualByComparingTo("4150");
        assertThat(fxRates().get(0).path("changedBy").asText())
                .as("who moved it")
                .isEqualTo("admin");

        saveRate("4150");
        assertThat(fxRates())
                .as("a history whose rows all say the same number is a log of saves")
                .hasSize(1);

        saveRate("4200");
        assertThat(fxRates().get(0).path("rate").decimalValue()).isEqualByComparingTo("4200");
    }

    private void saveRate(String rate) throws Exception {
        mvc.perform(put("/api/settings")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency.khrRate\":\"%s\"}".formatted(rate)))
                .andExpect(status().isOk());
    }

    private JsonNode fxRates() throws Exception {
        MvcResult res = mvc.perform(get("/api/settings/fx-rates")
                .header("Authorization", admin)).andReturn();
        return json.readTree(res.getResponse().getContentAsString()).path("data");
    }
}
