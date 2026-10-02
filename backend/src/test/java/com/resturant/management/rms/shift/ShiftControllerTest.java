package com.resturant.management.rms.shift;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resturant.management.rms.user.UserInfm;
import com.resturant.management.rms.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The drawer, and the rule that there is only ever one of them open.
 *
 * <p>Cash used to enter and leave a till that nobody opened or closed, so "we
 * are forty dollars short" had nothing to be short <em>of</em>. A shift is the
 * session that gives the question a frame.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShiftControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ShiftRepository shifts;
    @Autowired UserRepository users;

    private String cashier;
    private String admin;

    @BeforeEach
    void signIn() throws Exception {
        cashier = token("cashier");
        admin = token("admin");
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

    private long openShift(String bearer, String floatAmount) throws Exception {
        MvcResult res = mvc.perform(post("/api/shifts")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"openingFloat\":%s}".formatted(floatAmount)))
                .andExpect(status().isCreated())
                .andReturn();
        return data(res).path("id").asLong();
    }

    private void movement(long shiftId, String body) throws Exception {
        mvc.perform(post("/api/shifts/" + shiftId + "/movements")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    /** Sells 2 × fried rice (9.90 with VAT) at a table and settles it. */
    private void sell(long tableId, String payment) throws Exception {
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
                        .content("""
                                {"items":[{"productId":1,"qty":2}]}"""))
                .andExpect(status().isOk());

        mvc.perform(post("/api/orders/" + id + "/pay")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payment))
                .andExpect(status().isOk());
    }

    /* ---- One open shift ---------------------------------------------------- */

    @Test
    @DisplayName("a cashier who already has a shift open is told so")
    void secondShiftRefused() throws Exception {
        openShift(cashier, "100.00");

        mvc.perform(post("/api/shifts")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"openingFloat":50.00}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.shift.alreadyOpen"));
    }

    /**
     * The constraint itself, not the check in front of it. Two tills sending
     * the request at the same instant both pass the service check; this is
     * what stops the second one. ERD §3.2 wanted a partial index for this and
     * V14 explains why it is a nullable unique column instead — the point of
     * this test is that the substitution actually behaves the same way.
     */
    @Test
    @DisplayName("the database refuses a second open shift, not just the service")
    void databaseRefusesTwoOpenShifts() {
        UserInfm user = users.findByUserId("cashier").orElseThrow();
        shifts.saveAndFlush(CashShift.open(user, new BigDecimal("100.00")));

        assertThatThrownBy(() -> shifts.saveAndFlush(CashShift.open(user, new BigDecimal("50.00"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * The other half of that substitution: the key is null once a shift
     * closes, and a unique index has to allow many nulls or a cashier could
     * only ever work one shift in their life.
     */
    @Test
    @DisplayName("a cashier can open a shift again after closing the last one")
    void reopenAfterClosing() throws Exception {
        long first = openShift(cashier, "100.00");
        mvc.perform(post("/api/shifts/" + first + "/close")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"declaredCash":100.00}"""))
                .andExpect(status().isOk());

        long second = openShift(cashier, "80.00");
        assertThat(second).isNotEqualTo(first);

        // And a third, so this is not a test that passes for one closed row.
        mvc.perform(post("/api/shifts/" + second + "/close")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"declaredCash":80.00}"""))
                .andExpect(status().isOk());
        openShift(cashier, "60.00");
    }

    /* ---- Counting ---------------------------------------------------------- */

    @Test
    @DisplayName("expected cash is the float plus everything that moved")
    void expectedCashFollowsTheMovements() throws Exception {
        long id = openShift(cashier, "100.00");

        movement(id, """
                {"type":"PAY_IN","amount":25.00,"reason":"change from the safe"}""");
        movement(id, """
                {"type":"PAY_OUT","amount":10.00,"reason":"paid the ice delivery"}""");
        movement(id, """
                {"type":"DROP","amount":50.00,"reason":"to the safe"}""");

        MvcResult res = mvc.perform(get("/api/shifts/" + id)
                .header("Authorization", cashier)).andReturn();

        assertThat(data(res).path("shift").path("expectedCash").decimalValue())
                .as("100 + 25 - 10 - 50")
                .isEqualByComparingTo("65.00");
    }

    @Test
    @DisplayName("a cash sale goes into the drawer and a card sale does not")
    void onlyCashReachesTheDrawer() throws Exception {
        long id = openShift(cashier, "100.00");

        sell(2, """
                {"paymentMethod":"CASH","amountTendered":20.00}""");
        sell(3, """
                {"paymentMethod":"CARD"}""");

        MvcResult res = mvc.perform(get("/api/shifts/" + id)
                .header("Authorization", cashier)).andReturn();
        JsonNode shift = data(res).path("shift");

        assertThat(shift.path("cashSales").decimalValue())
                .as("the bill's total, not the twenty handed over")
                .isEqualByComparingTo("9.90");
        assertThat(shift.path("saleCount").asLong()).isEqualTo(1);
        assertThat(shift.path("expectedCash").decimalValue()).isEqualByComparingTo("109.90");
    }

    @Test
    @DisplayName("closing records the variance, and a short drawer needs a note")
    void closingNeedsAnExplanation() throws Exception {
        long id = openShift(cashier, "100.00");

        mvc.perform(post("/api/shifts/" + id + "/close")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"declaredCash":95.00}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.shift.shortNeedsNote"));

        MvcResult res = mvc.perform(post("/api/shifts/" + id + "/close")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"declaredCash":95.00,"note":"five dollars missing, nobody saw"}"""))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode closed = data(res);
        assertThat(closed.path("status").asText()).isEqualTo("CLOSED");
        assertThat(closed.path("expectedCash").decimalValue()).isEqualByComparingTo("100.00");
        assertThat(closed.path("declaredCash").decimalValue()).isEqualByComparingTo("95.00");
        assertThat(closed.path("variance").decimalValue())
                .as("negative is short")
                .isEqualByComparingTo("-5.00");
    }

    @Test
    @DisplayName("a drawer that balances closes without a note")
    void balancedClosesQuietly() throws Exception {
        long id = openShift(cashier, "100.00");

        mvc.perform(post("/api/shifts/" + id + "/close")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"declaredCash":100.00}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variance").value(0));
    }

    /** SALE follows from a bill. A second way to write one is a second answer. */
    @Test
    @DisplayName("a sale cannot be typed into the drawer by hand")
    void saleIsNotManual() throws Exception {
        long id = openShift(cashier, "100.00");

        mvc.perform(post("/api/shifts/" + id + "/movements")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"SALE","amount":10.00,"reason":"a bill I rang up"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.shift.typeNotManual"));
    }

    /* ---- The gate ----------------------------------------------------------- */

    /**
     * Without this the screen's redirect is the whole rule, and a redirect is
     * not a rule — it is a suggestion that an API call does not have to take.
     */
    @Test
    @DisplayName("a bill cannot be settled with no drawer open, and nothing changes when it is refused")
    void payingWithoutAShiftIsRefused() throws Exception {
        MvcResult opened = mvc.perform(post("/api/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tableId":8,"guestCount":2}"""))
                .andExpect(status().isCreated())
                .andReturn();
        long id = data(opened).path("id").asLong();

        mvc.perform(put("/api/orders/" + id + "/items")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":1,"qty":2}]}"""))
                .andExpect(status().isOk());

        mvc.perform(post("/api/orders/" + id + "/pay")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"CASH","amountTendered":20.00}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.shift.required"));

        // Taking the order was never gated — only settling it is — so the bill
        // is still there to be paid once somebody opens the till.
        mvc.perform(get("/api/orders/" + id).header("Authorization", cashier))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.payments.length()").value(0));
    }

    @Test
    @DisplayName("current shift is null rather than a 404 when there is none")
    void currentIsNullWhenClosed() throws Exception {
        mvc.perform(get("/api/shifts/current").header("Authorization", cashier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        openShift(cashier, "100.00");

        mvc.perform(get("/api/shifts/current").header("Authorization", cashier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.openingFloat").value(100.00));
    }

    @Test
    @DisplayName("shift history is for managers, not for every cashier")
    void historyIsManagerOnly() throws Exception {
        mvc.perform(get("/api/shifts").header("Authorization", admin))
                .andExpect(status().isOk());

        mvc.perform(get("/api/shifts").header("Authorization", cashier))
                .andExpect(status().isForbidden());
    }

    /**
     * The figure the count was measured against has to stay put. A movement
     * corrected after the fact must not change a variance somebody signed.
     */
    @Test
    @DisplayName("a closed shift keeps the expected figure it was closed against")
    void closedExpectedIsFrozen() throws Exception {
        long id = openShift(cashier, "100.00");
        movement(id, """
                {"type":"PAY_IN","amount":20.00,"reason":"float top-up"}""");

        mvc.perform(post("/api/shifts/" + id + "/close")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"declaredCash":120.00}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expectedCash").value(120.00));

        // A movement can no longer be added to a closed shift, which is the
        // first line of defence; the stored figure is the second.
        mvc.perform(post("/api/shifts/" + id + "/movements")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"PAY_IN","amount":5.00,"reason":"found behind the till"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.shift.notOpen"));

        MvcResult res = mvc.perform(get("/api/shifts/" + id)
                .header("Authorization", cashier)).andReturn();
        assertThat(data(res).path("shift").path("expectedCash").decimalValue())
                .isEqualByComparingTo("120.00");
    }
}
