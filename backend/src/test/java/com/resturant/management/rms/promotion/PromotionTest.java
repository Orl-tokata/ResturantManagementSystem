package com.resturant.management.rms.promotion;

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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Money coming off by a rule.
 *
 * <p>Everything here is about arithmetic a customer could check on the slip.
 * A wrong discount is a wrong price on something printed, which is why
 * SCREENS §3.5 held the complicated type back and why these tests are mostly
 * sums.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PromotionTest {

    /** Fried rice at 4.50, in category 1. VAT is 10%. */
    private static final long PRODUCT = 1;

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

    private static String iso(LocalDateTime when) {
        return when.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /** A rule running now, unless the caller says otherwise. */
    private MvcResult createPromotion(String body) throws Exception {
        return mvc.perform(post("/api/promotions")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private long livePromotion(String name, String type, String value,
                               String scope, Long scopeId, String minAmount) throws Exception {
        String body = """
                {"name":"%s","type":"%s","value":%s,"scope":"%s"%s%s,
                 "startsAt":"%s","endsAt":"%s"}"""
                .formatted(name, type, value, scope,
                        scopeId == null ? "" : ",\"scopeId\":" + scopeId,
                        minAmount == null ? "" : ",\"minAmount\":" + minAmount,
                        iso(LocalDateTime.now().minusDays(1)),
                        iso(LocalDateTime.now().plusDays(1)));
        MvcResult res = createPromotion(body);
        assertThat(res.getResponse().getStatus()).isEqualTo(201);
        return data(res).path("id").asLong();
    }

    /** Opens a bill with {@code qty} of the seeded dish and returns the result. */
    private MvcResult bill(long tableId, int qty) throws Exception {
        MvcResult opened = mvc.perform(post("/api/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableId\":%d,\"guestCount\":1}".formatted(tableId)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = data(opened).path("id").asLong();

        return mvc.perform(put("/api/orders/" + id + "/items")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":%d,\"qty\":%d}]}".formatted(PRODUCT, qty)))
                .andExpect(status().isOk())
                .andReturn();
    }

    /* ---- The arithmetic ----------------------------------------------------- */

    @Test
    @DisplayName("a percentage off one dish comes off its line, and VAT follows")
    void percentOffAnItem() throws Exception {
        livePromotion("Ten off rice", "PERCENT", "10", "ITEM", PRODUCT, null);

        JsonNode order = data(bill(1, 2));            // 9.00

        assertThat(order.path("subtotal").decimalValue()).isEqualByComparingTo("9.00");
        assertThat(order.path("promoDiscount").decimalValue())
                .as("ten percent of nine")
                .isEqualByComparingTo("0.90");
        assertThat(order.path("items").get(0).path("discountAmount").decimalValue())
                .isEqualByComparingTo("0.90");

        // VAT is charged on what is actually payable, not on the list price.
        assertThat(order.path("vatAmount").decimalValue()).isEqualByComparingTo("0.81");
        assertThat(order.path("total").decimalValue()).isEqualByComparingTo("8.91");
    }

    @Test
    @DisplayName("a fixed amount off the bill comes off after the line rules")
    void amountOffTheOrder() throws Exception {
        livePromotion("Two dollars off", "AMOUNT", "2.00", "ORDER", null, null);

        JsonNode order = data(bill(2, 2));            // 9.00

        assertThat(order.path("promoDiscount").decimalValue()).isEqualByComparingTo("2.00");
        assertThat(order.path("total").decimalValue())
                .as("(9.00 - 2.00) plus 10%")
                .isEqualByComparingTo("7.70");
    }

    /**
     * The compounding question, answered once and on purpose: a whole-bill
     * rule works on what is left after the line rules, not on the list price.
     */
    @Test
    @DisplayName("a bill rule is measured against what the line rules left")
    void orderRuleFollowsLineRules() throws Exception {
        livePromotion("Ten off rice", "PERCENT", "10", "ITEM", PRODUCT, null);
        livePromotion("Ten off everything", "PERCENT", "10", "ORDER", null, null);

        JsonNode order = data(bill(3, 2));            // 9.00

        // 0.90 off the line, then 10% of the remaining 8.10.
        assertThat(order.path("promoDiscount").decimalValue()).isEqualByComparingTo("1.71");
        assertThat(order.path("total").decimalValue()).isEqualByComparingTo("8.02");
    }

    @Test
    @DisplayName("when two rules could apply to a line, the customer gets the better one")
    void bestRuleWins() throws Exception {
        livePromotion("Small offer", "PERCENT", "5", "ITEM", PRODUCT, null);
        livePromotion("Better offer", "AMOUNT", "1.50", "ITEM", PRODUCT, null);

        JsonNode order = data(bill(5, 2));            // 9.00

        // 5% is 0.45; the fixed 1.50 is more, and stacking is not a thing.
        assertThat(order.path("promoDiscount").decimalValue()).isEqualByComparingTo("1.50");
    }

    @Test
    @DisplayName("a discount never exceeds what it is taken off")
    void discountIsCapped() throws Exception {
        livePromotion("Absurd", "AMOUNT", "100.00", "ITEM", PRODUCT, null);

        JsonNode order = data(bill(6, 1));            // 4.50

        assertThat(order.path("promoDiscount").decimalValue()).isEqualByComparingTo("4.50");
        assertThat(order.path("total").decimalValue())
                .as("free, and not less than free")
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("a minimum keeps a rule off bills that are too small")
    void minimumAmountIsRespected() throws Exception {
        livePromotion("Big spenders", "PERCENT", "10", "ORDER", null, "20.00");

        assertThat(data(bill(7, 1)).path("promoDiscount").decimalValue())
                .as("4.50 is under the floor")
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("a rule outside its dates does nothing")
    void expiredRuleDoesNothing() throws Exception {
        MvcResult res = createPromotion("""
                {"name":"Last month","type":"PERCENT","value":50,"scope":"ITEM","scopeId":%d,
                 "startsAt":"%s","endsAt":"%s"}"""
                .formatted(PRODUCT,
                        iso(LocalDateTime.now().minusDays(30)),
                        iso(LocalDateTime.now().minusDays(2))));
        assertThat(res.getResponse().getStatus()).isEqualTo(201);
        assertThat(data(res).path("liveNow").asBoolean()).isFalse();

        assertThat(data(bill(8, 2)).path("promoDiscount").decimalValue())
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("a rule that stops applying stops applying to an open bill")
    void ruleIsRecomputedNotRemembered() throws Exception {
        long id = livePromotion("Ten off rice", "PERCENT", "10", "ITEM", PRODUCT, null);

        MvcResult first = bill(9, 2);
        long orderId = data(first).path("id").asLong();
        assertThat(data(first).path("promoDiscount").decimalValue()).isEqualByComparingTo("0.90");

        // Switched off, then the basket is touched again.
        mvc.perform(put("/api/promotions/" + id)
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ten off rice","type":"PERCENT","value":10,
                                 "scope":"ITEM","scopeId":%d,"active":false,
                                 "startsAt":"%s","endsAt":"%s"}"""
                                .formatted(PRODUCT,
                                        iso(LocalDateTime.now().minusDays(1)),
                                        iso(LocalDateTime.now().plusDays(1)))))
                .andExpect(status().isOk());

        MvcResult again = mvc.perform(put("/api/orders/" + orderId + "/items")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":%d,\"qty\":2}]}".formatted(PRODUCT)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(data(again).path("promoDiscount").decimalValue())
                .as("the rule is recomputed every time, not remembered")
                .isEqualByComparingTo("0.00");
        assertThat(data(again).path("items").get(0).path("discountAmount").decimalValue())
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("what came off a bill can be listed, and it is what was charged")
    void applicableReportsWhatApplied() throws Exception {
        livePromotion("Ten off rice", "PERCENT", "10", "ITEM", PRODUCT, null);

        long orderId = data(bill(10, 2)).path("id").asLong();

        MvcResult res = mvc.perform(get("/api/promotions/applicable?orderId=" + orderId)
                        .header("Authorization", cashier))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode applied = data(res);
        assertThat(applied).hasSize(1);
        assertThat(applied.get(0).path("name").asText()).isEqualTo("Ten off rice");
        assertThat(applied.get(0).path("discount").decimalValue()).isEqualByComparingTo("0.90");
    }

    /* ---- The rules about rules ---------------------------------------------- */

    @Test
    @DisplayName("buy-X-get-Y is refused rather than half-built")
    void buyXGetYIsNotReady() throws Exception {
        MvcResult res = createPromotion("""
                {"name":"Two for one","type":"BUY_X_GET_Y","value":1,"scope":"ITEM","scopeId":%d,
                 "startsAt":"%s","endsAt":"%s"}"""
                .formatted(PRODUCT,
                        iso(LocalDateTime.now()),
                        iso(LocalDateTime.now().plusDays(1))));

        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(res.getResponse().getContentAsString()).path("code").asText())
                .isEqualTo("error.promotion.typeNotReady");
    }

    @Test
    @DisplayName("a rule that names nothing to apply to is refused")
    void scopeMustNameSomething() throws Exception {
        MvcResult res = createPromotion("""
                {"name":"Nothing in particular","type":"PERCENT","value":10,"scope":"ITEM",
                 "startsAt":"%s","endsAt":"%s"}"""
                .formatted(iso(LocalDateTime.now()), iso(LocalDateTime.now().plusDays(1))));

        assertThat(res.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(res.getResponse().getContentAsString()).path("code").asText())
                .isEqualTo("error.promotion.scopeIdRequired");
    }

    @Test
    @DisplayName("more than a hundred percent off is refused")
    void percentIsCapped() throws Exception {
        MvcResult res = createPromotion("""
                {"name":"Paying them","type":"PERCENT","value":150,"scope":"ORDER",
                 "startsAt":"%s","endsAt":"%s"}"""
                .formatted(iso(LocalDateTime.now()), iso(LocalDateTime.now().plusDays(1))));

        assertThat(res.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("a promotion a bill has used cannot be deleted")
    void usedPromotionsSurvive() throws Exception {
        long id = livePromotion("Ten off rice", "PERCENT", "10", "ITEM", PRODUCT, null);
        bill(11, 2);

        mvc.perform(delete("/api/promotions/" + id).header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.promotion.inUse"));
    }

    @Test
    @DisplayName("a cashier can see what is running but cannot write a rule")
    void cashiersRead() throws Exception {
        mvc.perform(get("/api/promotions/live").header("Authorization", cashier))
                .andExpect(status().isOk());

        mvc.perform(post("/api/promotions")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mine","type":"PERCENT","value":99,"scope":"ORDER",
                                 "startsAt":"%s","endsAt":"%s"}"""
                                .formatted(iso(LocalDateTime.now()),
                                        iso(LocalDateTime.now().plusDays(1)))))
                .andExpect(status().isForbidden());
    }
}
