package com.resturant.management.rms.customer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.resturant.management.rms.order.Order;
import com.resturant.management.rms.order.OrderRepository;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Who the bill belongs to, and what that is worth to them.
 *
 * <p>The balance is never a column. Everything here that asserts a number of
 * points is asserting a sum over the ledger, which is the only way the two can
 * never disagree.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CustomerRepository customers;
    @Autowired LoyaltyRepository loyalty;
    @Autowired OrderRepository orders;

    private String admin;
    private String cashier;

    @BeforeEach
    void signIn() throws Exception {
        admin = token("admin");
        cashier = token("cashier");
        openShift(cashier);
    }

    private String token(String username) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"ChangeMe123!\"}".formatted(username)))
                .andReturn();
        return "Bearer " + json.readTree(res.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    /** The P3 gate: no settled bill without a drawer open. */
    private void openShift(String bearer) throws Exception {
        mvc.perform(post("/api/shifts")
                .header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"openingFloat":200.00}"""));
    }

    private JsonNode data(MvcResult res) throws Exception {
        return json.readTree(res.getResponse().getContentAsString()).path("data");
    }

    private JsonNode register(String name, String phone) throws Exception {
        MvcResult res = mvc.perform(post("/api/customers")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"phone\":\"%s\"}".formatted(name, phone)))
                .andExpect(status().isCreated())
                .andReturn();
        return data(res);
    }

    private JsonNode customer(long id) throws Exception {
        return data(mvc.perform(get("/api/customers/" + id)
                .header("Authorization", admin)).andReturn());
    }

    /**
     * Sells 2 × fried rice (9.90 with VAT) to the given customer and settles
     * it in cash. The customer is named on the open bill, which is when a
     * cashier actually asks.
     */
    private long sellTo(long tableId, Long customerId) throws Exception {
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

        if (customerId != null) {
            mvc.perform(put("/api/orders/" + id + "/customer")
                            .header("Authorization", cashier)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"customerId\":%d}".formatted(customerId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.customerId").value(customerId));
        }

        mvc.perform(post("/api/orders/" + id + "/pay")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"CASH","amountTendered":20.00}"""))
                .andExpect(status().isOk());
        return id;
    }

    /* ---- Registering ------------------------------------------------------- */

    @Test
    @DisplayName("a registered customer gets a generated code and no points")
    void registerGeneratesCode() throws Exception {
        JsonNode c = register("Sok Dara", "012 345 678");

        assertThat(c.path("code").asText())
                .as("generated, so nobody has to invent one with a queue waiting")
                .startsWith("C-");
        assertThat(c.path("points").decimalValue()).isEqualByComparingTo("0");
        assertThat(c.path("visitCount").asLong()).isZero();
        assertThat(c.path("lastVisit").isMissingNode()).isTrue();
    }

    /**
     * A number shared by a couple, or reassigned by the carrier. ERD §3.4
     * refuses to make this unique on purpose; this is that decision, asserted.
     */
    @Test
    @DisplayName("two customers may share a phone number, and the lookup returns both")
    void phoneIsNotUnique() throws Exception {
        register("Chan Nita", "077 111 222");
        register("Chan Sophea", "077 111 222");

        MvcResult res = mvc.perform(get("/api/customers/lookup?phone=077 111 222")
                        .header("Authorization", cashier))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(data(res)).as("both of them, not a guess at which").hasSize(2);
    }

    @Test
    @DisplayName("the lookup is an exact match, not a search")
    void lookupIsExact() throws Exception {
        register("Ly Vuthy", "092 888 444");

        mvc.perform(get("/api/customers/lookup?phone=092").header("Authorization", cashier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("the list searches name, phone and code")
    void listSearches() throws Exception {
        register("Pich Sokha", "011 999 888");

        for (String q : new String[]{"Sokha", "011 999", "C-"}) {
            mvc.perform(get("/api/customers?search=" + q).header("Authorization", admin))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(
                            org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        }
    }

    /* ---- Earning ------------------------------------------------------------ */

    @Test
    @DisplayName("settling a bill gives its customer points, and records which bill")
    void settlingEarnsPoints() throws Exception {
        long id = register("Keo Pisey", "016 222 333").path("id").asLong();

        sellTo(1, id);

        // One point per dollar by default, over a 9.90 bill.
        assertThat(customer(id).path("points").decimalValue()).isEqualByComparingTo("9.90");

        MvcResult res = mvc.perform(get("/api/customers/" + id + "/loyalty")
                .header("Authorization", admin)).andReturn();
        JsonNode rows = data(res).path("content");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).path("type").asText()).isEqualTo("EARN");
        assertThat(rows.get(0).path("invoiceNo").asText())
                .as("which meal earned them")
                .startsWith("INV-");
    }

    @Test
    @DisplayName("a bill with no customer earns nobody anything")
    void anonymousBillEarnsNothing() throws Exception {
        long id = register("Not Here", "015 000 111").path("id").asLong();

        sellTo(3, null);

        assertThat(customer(id).path("points").decimalValue()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("the customer's totals follow their settled bills")
    void totalsFollowTheBills() throws Exception {
        long id = register("Regular Customer", "010 777 666").path("id").asLong();

        sellTo(5, id);
        sellTo(6, id);

        JsonNode c = customer(id);
        assertThat(c.path("visitCount").asLong()).isEqualTo(2);
        assertThat(c.path("totalSpent").decimalValue()).isEqualByComparingTo("19.80");
        assertThat(c.path("lastVisit").isMissingNode()).isFalse();
        assertThat(c.path("points").decimalValue()).isEqualByComparingTo("19.80");
    }

    /**
     * Earning twice for one meal is permanent, because the ledger is the
     * balance — there is no stored total to correct, only a row that should
     * not exist. The service checks first so a second attempt reads as a
     * no-op; this is the constraint underneath, which is what holds if a
     * future code path forgets to check.
     */
    @Test
    @DisplayName("the database refuses a second EARN against one bill")
    void aBillCannotEarnTwice() throws Exception {
        long id = register("Paid Once", "012 909 909").path("id").asLong();
        long orderId = sellTo(9, id);

        Customer customer = customers.findById(id).orElseThrow();
        Order order = orders.findById(orderId).orElseThrow();

        assertThatThrownBy(() -> loyalty.saveAndFlush(LoyaltyTransaction.builder()
                .customer(customer)
                .order(order)
                .type(LoyaltyType.EARN)
                .points(new java.math.BigDecimal("9.90"))
                .createdAt(java.time.LocalDateTime.now())
                .build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * The customer's purchase history is the order list with one more filter.
     *
     * <p>Worth its own test because of how that filter is written: a bare
     * `:param IS NULL OR column = :param` is accepted by H2 and rejected by
     * PostgreSQL, which is how the audit screen reached production answering
     * 500. This one casts, like the date bounds beside it, and the CI postgres
     * job is what proves the cast.
     */
    @Test
    @DisplayName("order history can be narrowed to one customer")
    void historyFiltersByCustomer() throws Exception {
        long mine = register("Mine Only", "011 343 343").path("id").asLong();
        long theirs = register("Someone Else", "011 565 565").path("id").asLong();

        sellTo(10, mine);
        sellTo(11, theirs);

        MvcResult res = mvc.perform(get("/api/orders?customerId=" + mine)
                        .header("Authorization", admin))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode rows = data(res).path("content");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).path("customerId").asLong()).isEqualTo(mine);
        assertThat(rows.get(0).path("customerName").asText()).isEqualTo("Mine Only");
    }

    @Test
    @DisplayName("naming the wrong customer can be undone before the bill is paid")
    void customerCanBeCleared() throws Exception {
        long id = register("Wrong Person", "011 787 787").path("id").asLong();

        MvcResult opened = mvc.perform(post("/api/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tableId":12,"guestCount":2}"""))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = data(opened).path("id").asLong();

        mvc.perform(put("/api/orders/" + orderId + "/customer")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":%d}".formatted(id)))
                .andExpect(jsonPath("$.data.customerId").value(id));

        mvc.perform(put("/api/orders/" + orderId + "/customer")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":null}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerId").doesNotExist());
    }

    /* ---- Adjusting ---------------------------------------------------------- */

    @Test
    @DisplayName("an adjustment moves the balance and says who and why")
    void adjustmentIsOnTheRecord() throws Exception {
        long id = register("Mistake Was Made", "069 123 123").path("id").asLong();

        mvc.perform(post("/api/customers/" + id + "/loyalty")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"points":50,"note":"goodwill after a long wait"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.createdBy").value("admin"));

        assertThat(customer(id).path("points").decimalValue()).isEqualByComparingTo("50");

        // And it goes the other way, which is the case a balance column would
        // quietly get wrong.
        mvc.perform(post("/api/customers/" + id + "/loyalty")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"points":-20,"note":"credited twice"}"""))
                .andExpect(status().isCreated());

        assertThat(customer(id).path("points").decimalValue()).isEqualByComparingTo("30");
    }

    @Test
    @DisplayName("an adjustment without a reason is refused")
    void adjustmentNeedsAReason() throws Exception {
        long id = register("No Reason", "069 000 000").path("id").asLong();

        mvc.perform(post("/api/customers/" + id + "/loyalty")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"points":50}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("only an admin may move somebody's points")
    void adjustmentIsAdminOnly() throws Exception {
        long id = register("Hands Off", "069 555 555").path("id").asLong();

        mvc.perform(post("/api/customers/" + id + "/loyalty")
                        .header("Authorization", cashier)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"points":500,"note":"for me"}"""))
                .andExpect(status().isForbidden());
    }

    /* ---- Deleting ----------------------------------------------------------- */

    @Test
    @DisplayName("a customer with history cannot be deleted")
    void historyBlocksDeletion() throws Exception {
        long fresh = register("Never Came Back", "070 111 111").path("id").asLong();
        mvc.perform(delete("/api/customers/" + fresh).header("Authorization", admin))
                .andExpect(status().isOk());

        long regular = register("Comes Often", "070 222 222").path("id").asLong();
        sellTo(8, regular);

        mvc.perform(delete("/api/customers/" + regular).header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("error.customer.hasHistory"));
    }
}
