package com.resturant.management.rms.khqr;

import com.resturant.management.rms.khqr.BakongClient.PaymentStatus.State;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * How the till reads the bank's answer.
 *
 * <p>Every case here decides whether an order gets closed, so the interesting
 * ones are the failures. Treating a timeout as "not paid" would leave a paying
 * customer holding a receipt the till never printed; treating it as "paid"
 * would hand away food. Both are wrong, and the only correct answer to a
 * question that could not be asked is that nothing is known.
 */
class BakongClientTest {

    private MockWebServer bakong;

    @BeforeEach
    void start() throws IOException {
        bakong = new MockWebServer();
        bakong.start();
    }

    @AfterEach
    void stop() throws IOException {
        bakong.shutdown();
    }

    private BakongClient clientWith(String token) {
        KhqrProperties props = new KhqrProperties(
                true, "shop@aclb", "Shop", "Phnom Penh", null, null, "POS-01",
                bakong.url("/").toString(), token, Duration.ofMinutes(5), Duration.ofSeconds(2));
        return new BakongClient(props, RestClient.builder());
    }

    @Test
    @DisplayName("a settled transaction comes back PAID, with who paid it")
    void settled() throws InterruptedException {
        bakong.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"responseCode":0,"responseMessage":"Getting transaction successfully",
                         "data":{"hash":"abc123","fromAccountId":"customer@wing",
                                 "toAccountId":"shop@aclb","amount":15.95,"currency":"USD"}}"""));

        var status = clientWith("token").check("d41d8cd98f00b204e9800998ecf8427e");

        assertThat(status.state()).isEqualTo(State.PAID);
        assertThat(status.payer()).isEqualTo("customer@wing");
        assertThat(status.reference()).isEqualTo("abc123");

        // The md5 is what identifies the transaction, so it has to be sent.
        RecordedRequest sent = bakong.takeRequest();
        assertThat(sent.getPath()).isEqualTo("/v1/check_transaction_by_md5");
        assertThat(sent.getHeader("Authorization")).isEqualTo("Bearer token");
        assertThat(sent.getBody().readUtf8()).contains("d41d8cd98f00b204e9800998ecf8427e");
    }

    @Test
    @DisplayName("an unpaid code is NOT_PAID, which is the ordinary answer while someone is still paying")
    void notYetPaid() {
        bakong.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"responseCode":1,"responseMessage":"Transaction could not be found",
                         "errorCode":1}"""));

        var status = clientWith("token").check("whatever");

        assertThat(status.state()).isEqualTo(State.NOT_PAID);
        assertThat(status.detail()).contains("could not be found");
    }

    @Test
    @DisplayName("an unreachable bank is UNKNOWN, never NOT_PAID")
    void networkFailureIsNotAnAnswer() throws IOException {
        // The bank is simply not there. Nothing can be concluded about the
        // money, so nothing about the order may change.
        bakong.shutdown();

        var status = clientWith("token").check("whatever");

        assertThat(status.state()).isEqualTo(State.UNKNOWN);
    }

    @Test
    @DisplayName("a server error is UNKNOWN too")
    void serverErrorIsNotAnAnswer() {
        bakong.enqueue(new MockResponse().setResponseCode(503));

        assertThat(clientWith("token").check("whatever").state()).isEqualTo(State.UNKNOWN);
    }

    @Test
    @DisplayName("a garbled body is UNKNOWN rather than an optimistic guess")
    void garbledBodyIsNotAnAnswer() {
        bakong.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("not json at all"));

        assertThat(clientWith("token").check("whatever").state()).isEqualTo(State.UNKNOWN);
    }

    @Test
    @DisplayName("with no token the bank is never asked, and the till says so")
    void withoutATokenNothingIsAsked() {
        // The distinction matters on screen: UNVERIFIABLE means a human must
        // decide, and the cashier needs to be told that rather than shown a
        // spinner that will never resolve.
        var status = clientWith(null).check("whatever");

        assertThat(status.state()).isEqualTo(State.UNVERIFIABLE);
        assertThat(bakong.getRequestCount()).isZero();
    }
}
