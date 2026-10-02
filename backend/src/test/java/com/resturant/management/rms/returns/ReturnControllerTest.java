package com.resturant.management.rms.returns;

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

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Money going back.
 *
 * <p>Five things have to happen together: the return document, a stock
 * movement per line, cash out of the drawer, a reversal against the payment,
 * and the customer's points. Any one of them landing without the others leaves
 * the books saying something untrue, so most of what is asserted here is that
 * the others moved too.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReturnControllerTest {

    /** Fried rice: 4.50 each, VAT 10%. */
    private static final long PRODUCT = 1;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String admin;
    private String cashier;

    @BeforeEach
    void signIn() throws Exception {
        admin = token("admin");
        cashier = token("cashier");
        openShift(cashier);
        openShift(admin);
    }

    private String token(String username) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"ChangeMe123!\"}".formatted(username)))
                .andReturn();
        return "Bearer " + json.readTree(res.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private void openShift(String bearer) throws Exception {
        mvc.perform(post("/api/shifts")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"openingFloat":500.00}"""));
    }

    private JsonNode data(MvcResult res) throws Exception {
        return json.readTree(res.getResponse().getContentAsString()).path("data");
    }

    /** Sells {@code qty} of fried rice at a table, settles in cash, returns the bill. */
    private JsonNode sell(long tableId, int qty, Long customerId) throws Exception {
        MvcResult opened = mvc.perform(post("/api/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableId\":%d,\"guestCount\":2}".formatted(tableId)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = data(opened).path("id").asLong();

        mvc.perform(put("/api/orders/" + id + "/items")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":%d,\"qty\":%d}]}".formatted(PRODUCT, qty)))
                .andExpect(status().isOk());

        if (customerId != null) {
            mvc.perform(put("/api/orders/" + id + "/customer")
                    .header("Authorization", cashier)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"customerId\":%d}".formatted(customerId)))
                    .andExpect(status().isOk());
        }

        MvcResult paid = mvc.perform(post("/api/orders/" + id + "/pay")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"CASH","amountTendered":500}"""))
                .andExpect(status().isOk())
                .andReturn();
        return data(paid);
    }

    private JsonNode returnable(long orderId) throws Exception {
        return data(mvc.perform(get("/api/orders/" + orderId + "/returnable")
                .header("Authorization", cashier)).andReturn());
    }

    private long lineIdOf(JsonNode order) {
        return order.path("items").get(0).path("id").asLong();
    }

    private BigDecimal stockOf(long productId) throws Exception {
        MvcResult res = mvc.perform(get("/api/products/" + productId)
                .header("Authorization", admin)).andReturn();
        return data(res).path("stockQty").decimalValue();
    }

    private MvcResult refund(String bearer, String body) throws Exception {
        return mvc.perform(post("/api/returns")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    /* ---- What remains ------------------------------------------------------ */

    @Test
    @DisplayName("a fresh sale has everything still returnable")
    void nothingReturnedYet() throws Exception {
        JsonNode order = sell(1, 3, null);

        JsonNode r = returnable(order.path("id").asLong());
        assertThat(r.path("anythingLeft").asBoolean()).isTrue();
        assertThat(r.path("originalMethod").asText()).isEqualTo("CASH");

        JsonNode line = r.path("lines").get(0);
        assertThat(line.path("soldQty").decimalValue()).isEqualByComparingTo("3");
        assertThat(line.path("returnedQty").decimalValue()).isEqualByComparingTo("0");
        assertThat(line.path("remainingQty").decimalValue()).isEqualByComparingTo("3");
    }

    @Test
    @DisplayName("an unsettled bill has nothing to give back")
    void openBillCannotBeReturned() throws Exception {
        MvcResult opened = mvc.perform(post("/api/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tableId":2,"guestCount":1}"""))
                .andExpect(status().isCreated())
                .andReturn();

        mvc.perform(get("/api/orders/" + data(opened).path("id").asLong() + "/returnable")
                        .header("Authorization", cashier))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.return.notPaid"));
    }

    /* ---- The five things --------------------------------------------------- */

    /**
     * The whole package in one test, because the point of P7 is that these
     * happen together. A stock movement without a cash movement is a drawer
     * that cannot be counted; a refund without the stock is food that was paid
     * for twice.
     */
    @Test
    @DisplayName("a return restocks, pays out of the drawer, reverses the payment and the points")
    void aReturnMovesEverything() throws Exception {
        MvcResult customer = mvc.perform(post("/api/customers")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Returns Often","phone":"012 500 500"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        long customerId = data(customer).path("id").asLong();

        BigDecimal stockBefore = stockOf(PRODUCT);
        JsonNode order = sell(3, 2, customerId);      // 9.00 + 10% = 9.90
        long orderId = order.path("id").asLong();
        long lineId = lineIdOf(order);

        // Points for the whole meal, before any of it goes back.
        assertThat(pointsOf(customerId)).isEqualByComparingTo("9.90");

        MvcResult res = refund(cashier, """
                {"orderId":%d,"lines":[{"orderItemId":%d,"qty":1}],
                 "reason":"the fish was off"}""".formatted(orderId, lineId));
        assertThat(res.getResponse().getStatus()).isEqualTo(201);

        JsonNode ret = data(res);
        assertThat(ret.path("returnNo").asText()).startsWith("RET-");
        // One of two dishes, so half the bill: 4.95, not the 4.50 menu price —
        // the customer paid VAT on it and gets that back too.
        assertThat(ret.path("total").decimalValue()).isEqualByComparingTo("4.95");
        assertThat(ret.path("refundMethod").asText())
                .as("however it was paid, unless told otherwise")
                .isEqualTo("CASH");

        // 1. the dish is back on the shelf
        assertThat(stockOf(PRODUCT)).isEqualByComparingTo(stockBefore.subtract(BigDecimal.ONE));

        // 2. the payment carries a reversal, and the bill still reads as paid
        MvcResult after = mvc.perform(get("/api/orders/" + orderId)
                .header("Authorization", cashier)).andReturn();
        JsonNode payments = data(after).path("payments");
        assertThat(payments).hasSize(2);
        assertThat(payments.get(1).path("status").asText()).isEqualTo("REFUNDED");
        assertThat(payments.get(1).path("amount").decimalValue()).isEqualByComparingTo("4.95");
        assertThat(data(after).path("status").asText()).isEqualTo("PAID");

        // 3. the cash left the drawer
        MvcResult shift = mvc.perform(get("/api/shifts/current")
                .header("Authorization", cashier)).andReturn();
        long shiftId = data(shift).path("id").asLong();
        MvcResult detail = mvc.perform(get("/api/shifts/" + shiftId)
                .header("Authorization", cashier)).andReturn();
        JsonNode refundMovement = null;
        for (JsonNode m : data(detail).path("movements")) {
            if ("REFUND".equals(m.path("type").asText())) refundMovement = m;
        }
        assertThat(refundMovement).as("a REFUND against the open shift").isNotNull();
        assertThat(refundMovement.path("amount").decimalValue()).isEqualByComparingTo("4.95");
        assertThat(refundMovement.path("increase").asBoolean()).isFalse();

        // 4. and the points went back with it — half the meal, half the points
        assertThat(pointsOf(customerId)).isEqualByComparingTo("4.95");

        // 5. the line now shows one of two already returned
        JsonNode line = returnable(orderId).path("lines").get(0);
        assertThat(line.path("returnedQty").decimalValue()).isEqualByComparingTo("1");
        assertThat(line.path("remainingQty").decimalValue()).isEqualByComparingTo("1");
    }

    private BigDecimal pointsOf(long customerId) throws Exception {
        MvcResult res = mvc.perform(get("/api/customers/" + customerId)
                .header("Authorization", admin)).andReturn();
        return data(res).path("points").decimalValue();
    }

    /* ---- Over-returning ----------------------------------------------------- */

    /**
     * The check ERD §3.7 says no constraint can carry: three of a line of two,
     * across two documents. It only shows up as a sum.
     */
    @Test
    @DisplayName("more than remains is refused, across documents as well as within one")
    void cannotReturnMoreThanWasSold() throws Exception {
        JsonNode order = sell(5, 2, null);
        long orderId = order.path("id").asLong();
        long lineId = lineIdOf(order);

        mvc.perform(post("/api/returns")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":%d,"lines":[{"orderItemId":%d,"qty":3}],
                                 "reason":"too many"}""".formatted(orderId, lineId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("error.return.exceedsSold"));

        // Now two separate documents that together exceed the line.
        assertThat(refund(cashier, """
                {"orderId":%d,"lines":[{"orderItemId":%d,"qty":2}],
                 "reason":"both of them"}""".formatted(orderId, lineId))
                .getResponse().getStatus()).isEqualTo(201);

        mvc.perform(post("/api/returns")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":%d,"lines":[{"orderItemId":%d,"qty":1}],
                                 "reason":"one more"}""".formatted(orderId, lineId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("error.return.exceedsSold"));
    }

    /**
     * The case the first version of this suite missed, and the browser caught.
     *
     * <p>V15 put a unique index on {@code (order_id, type)} to stop a meal
     * earning twice. Every return also writes a REVERSE row against the order,
     * so the second partial refund on one bill hit that index and came back as
     * a bare 409. Nothing here noticed, because every test that returned twice
     * used a bill with no customer and so wrote no REVERSE rows at all.
     *
     * <p>A customer is the whole point of this test.
     */
    @Test
    @DisplayName("a bill with a customer can be returned twice, a line at a time")
    void twoPartialReturnsOnOneBill() throws Exception {
        MvcResult customer = mvc.perform(post("/api/customers")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Twice Over","phone":"012 600 600"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        long customerId = data(customer).path("id").asLong();

        JsonNode order = sell(12, 2, customerId);      // 9.90, earning 9.90
        long orderId = order.path("id").asLong();
        long lineId = lineIdOf(order);

        String one = """
                {"orderId":%d,"lines":[{"orderItemId":%d,"qty":1}],
                 "reason":"the first one"}""".formatted(orderId, lineId);

        assertThat(refund(cashier, one).getResponse().getStatus()).isEqualTo(201);
        assertThat(pointsOf(customerId)).isEqualByComparingTo("4.95");

        assertThat(refund(cashier, """
                {"orderId":%d,"lines":[{"orderItemId":%d,"qty":1}],
                 "reason":"and the other"}""".formatted(orderId, lineId))
                .getResponse().getStatus())
                .as("a second return against the same bill")
                .isEqualTo(201);

        assertThat(pointsOf(customerId))
                .as("the whole meal came back, so all of its points did")
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("a line from another bill is refused")
    void lineMustBelongToTheBill() throws Exception {
        JsonNode first = sell(6, 1, null);
        JsonNode second = sell(7, 1, null);

        mvc.perform(post("/api/returns")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":%d,"lines":[{"orderItemId":%d,"qty":1}],
                                 "reason":"wrong bill"}"""
                                .formatted(first.path("id").asLong(), lineIdOf(second))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.return.lineNotOnBill"));
    }

    @Test
    @DisplayName("a return without a reason is refused")
    void reasonIsMandatory() throws Exception {
        JsonNode order = sell(8, 1, null);

        mvc.perform(post("/api/returns")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":%d,"lines":[{"orderItemId":%d,"qty":1}]}"""
                                .formatted(order.path("id").asLong(), lineIdOf(order))))
                .andExpect(status().isBadRequest());
    }

    /* ---- Approval ----------------------------------------------------------- */

    /**
     * SCREENS §3.4. The threshold is seeded at 20, so eight dishes is over it
     * and one is not.
     */
    @Test
    @DisplayName("a large refund needs a manager, and records who approved it")
    void largeRefundNeedsApproval() throws Exception {
        JsonNode order = sell(9, 8, null);        // 36.00 + VAT = 39.60
        long orderId = order.path("id").asLong();
        long lineId = lineIdOf(order);

        String body = """
                {"orderId":%d,"lines":[{"orderItemId":%d,"qty":8}],
                 "reason":"the whole table sent it back"}""".formatted(orderId, lineId);

        mvc.perform(post("/api/returns")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("error.return.approvalRequired"));

        MvcResult res = refund(admin, body);
        assertThat(res.getResponse().getStatus()).isEqualTo(201);
        assertThat(data(res).path("approvedBy").asText())
                .as("a refund needs a name against it")
                .isEqualTo("Administrator");
    }

    @Test
    @DisplayName("a small refund does not, and records no approver")
    void smallRefundNeedsNobody() throws Exception {
        JsonNode order = sell(10, 1, null);

        MvcResult res = refund(cashier, """
                {"orderId":%d,"lines":[{"orderItemId":%d,"qty":1}],
                 "reason":"cold"}""".formatted(order.path("id").asLong(), lineIdOf(order)));

        assertThat(res.getResponse().getStatus()).isEqualTo(201);
        assertThat(data(res).path("approvedBy").isMissingNode()).isTrue();
    }

    /* ---- The drawer --------------------------------------------------------- */

    @Test
    @DisplayName("a card refund touches no drawer and needs no shift")
    void cardRefundDoesNotTouchTheDrawer() throws Exception {
        JsonNode order = sell(11, 1, null);

        MvcResult res = refund(cashier, """
                {"orderId":%d,"lines":[{"orderItemId":%d,"qty":1}],
                 "reason":"back to the card","refundMethod":"CARD"}"""
                .formatted(order.path("id").asLong(), lineIdOf(order)));

        assertThat(res.getResponse().getStatus()).isEqualTo(201);
        assertThat(data(res).path("refundMethod").asText()).isEqualTo("CARD");
        assertThat(data(res).path("shiftId").isMissingNode())
                .as("no drawer was involved")
                .isTrue();
    }
}
