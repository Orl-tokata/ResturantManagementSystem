package com.resturant.management.rms.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resturant.management.rms.khqr.BakongClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * What a QR code leaves behind.
 *
 * <p>Before V12 a code that was shown and never paid left nothing at all: the
 * order carried a payment_method of KHQR while it was on screen, and
 * abandoning it set that back to null. "We showed three codes and took one
 * payment" was not a question the database could answer.
 *
 * <p>KHQR is off in this suite's configuration, because there is no account to
 * pay into. Generating a code needs no bank — it is a published format — so
 * the only thing standing in for one here is the half that cannot be faked:
 * whether the money arrived.
 */
@SpringBootTest(properties = {
        "app.khqr.enabled=true",
        "app.khqr.account-id=test@aclb",
        "app.khqr.merchant-name=Test Restaurant",
        "app.khqr.api-token=test-token",
})
@AutoConfigureMockMvc
@Transactional
class KhqrPaymentTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired SalePaymentRepository payments;

    @MockitoBean BakongClient bakong;

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
        openShift("Bearer " + token);
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


    /** A bill of 2 × fried rice = 9.00, 9.90 with VAT. */
    private long bill(long tableId) throws Exception {
        MvcResult opened = mvc.perform(post("/api/orders")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tableId\":%d,\"guestCount\":2}".formatted(tableId)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = json.readTree(opened.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mvc.perform(put("/api/orders/" + id + "/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"productId":1,"qty":2}]}"""))
                .andExpect(status().isOk());
        return id;
    }

    private void showCode(long orderId) throws Exception {
        mvc.perform(post("/api/orders/" + orderId + "/khqr")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private JsonNode order(long id) throws Exception {
        MvcResult res = mvc.perform(get("/api/orders/" + id)
                .header("Authorization", "Bearer " + token)).andReturn();
        return json.readTree(res.getResponse().getContentAsString()).path("data");
    }

    /* ---- Shown ------------------------------------------------------------ */

    @Test
    @DisplayName("showing a code records a pending payment carrying its md5")
    void shownCodeIsPending() throws Exception {
        long id = bill(1);
        showCode(id);

        JsonNode o = order(id);
        assertThat(o.path("status").asText()).isEqualTo("AWAITING_PAYMENT");
        assertThat(o.path("paymentMethod").isMissingNode())
                .as("no money has arrived, so the bill has no method yet")
                .isTrue();

        assertThat(o.path("payments")).hasSize(1);
        assertThat(o.path("payments").get(0).path("status").asText()).isEqualTo("PENDING");
        assertThat(o.path("payments").get(0).path("method").asText()).isEqualTo("KHQR");

        SalePayment row = payments.findByOrderIdOrderByIdAsc(id).get(0);
        assertThat(row.getKhqrMd5()).as("the identifier the bank will be asked about").isNotBlank();
    }

    @Test
    @DisplayName("asking again for a live code does not record a second attempt")
    void reaskingKeepsOneAttempt() throws Exception {
        long id = bill(3);
        showCode(id);
        showCode(id);

        assertThat(payments.findByOrderIdOrderByIdAsc(id))
                .as("the same code redrawn is the same attempt")
                .hasSize(1);
    }

    /* ---- Paid -------------------------------------------------------------- */

    @Test
    @DisplayName("the bank confirming turns the pending row into the payment")
    void bankConfirmationCapturesTheRow() throws Exception {
        long id = bill(5);
        showCode(id);

        when(bakong.check(anyString())).thenReturn(new BakongClient.PaymentStatus(
                BakongClient.PaymentStatus.State.PAID,
                new BigDecimal("9.90"), "USD", "SOK DARA", "FT25123ABCD", null));

        mvc.perform(get("/api/orders/" + id + "/khqr")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.state").value("PAID"));

        JsonNode o = order(id);
        assertThat(o.path("status").asText()).isEqualTo("PAID");
        assertThat(o.path("paymentMethod").asText()).isEqualTo("KHQR");

        assertThat(o.path("payments")).as("promoted, not duplicated").hasSize(1);
        JsonNode paid = o.path("payments").get(0);
        assertThat(paid.path("status").asText()).isEqualTo("CAPTURED");
        assertThat(paid.path("amount").asDouble()).isEqualTo(9.90);
        assertThat(paid.path("reference").asText())
                .as("what makes the line reconcilable against a statement")
                .isEqualTo("FT25123ABCD");
        assertThat(paid.path("tendered").isMissingNode())
                .as("nothing was handed over to a QR code")
                .isTrue();
    }

    /* ---- Not paid ---------------------------------------------------------- */

    @Test
    @DisplayName("abandoning a code records the attempt as failed, not as nothing")
    void abandonedCodeIsFailed() throws Exception {
        long id = bill(6);
        showCode(id);

        mvc.perform(delete("/api/orders/" + id + "/khqr")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode o = order(id);
        assertThat(o.path("status").asText()).as("back on the floor").isEqualTo("OPEN");
        assertThat(o.path("payments")).hasSize(1);
        assertThat(o.path("payments").get(0).path("status").asText()).isEqualTo("FAILED");
        assertThat(o.path("paymentMethod").isMissingNode())
                .as("a failed attempt is not a payment")
                .isTrue();
    }

    /**
     * The sequence a till actually produces: a code nobody scans, then cash.
     * Both are on the record, and only one of them is money.
     */
    @Test
    @DisplayName("a bill abandoned from QR and settled in cash keeps both rows")
    void abandonedThenPaidInCash() throws Exception {
        long id = bill(8);
        showCode(id);

        mvc.perform(delete("/api/orders/" + id + "/khqr")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(post("/api/orders/" + id + "/pay")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod":"CASH","amountTendered":10.00}"""))
                .andExpect(status().isOk());

        JsonNode o = order(id);
        assertThat(o.path("payments")).hasSize(2);
        assertThat(o.path("payments").get(0).path("status").asText()).isEqualTo("FAILED");
        assertThat(o.path("payments").get(1).path("status").asText()).isEqualTo("CAPTURED");
        assertThat(o.path("paymentMethod").asText())
                .as("one captured payment, so the bill has one method")
                .isEqualTo("CASH");
    }
}
