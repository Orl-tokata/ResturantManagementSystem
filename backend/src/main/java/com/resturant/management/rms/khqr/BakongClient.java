package com.resturant.management.rms.khqr;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Asks Bakong whether a given QR was actually paid.
 *
 * <p>This is the half of KHQR that cannot be faked. Generating a code needs
 * nothing from anyone; knowing that money arrived needs the bank to say so.
 * Until this answers yes, an order has been <em>shown</em> a code, not paid.
 *
 * <p>The API identifies a transaction by the MD5 of the QR payload, which is
 * why {@link Khqr} carries one.
 */
@Slf4j
@Component
public class BakongClient {

    private final KhqrProperties properties;
    private final RestClient http;

    public BakongClient(KhqrProperties properties, RestClient.Builder builder) {
        this.properties = properties;

        // Timeouts, not defaults. A till that hangs because a bank API is slow
        // is worse than one that reports "not yet" and lets the cashier retry.
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.requestTimeout().toMillis());
        factory.setReadTimeout((int) properties.requestTimeout().toMillis());

        this.http = builder
                .baseUrl(properties.apiBaseUrl())
                .requestFactory(factory)
                .build();
    }

    /**
     * @return what Bakong says about this payment
     */
    public PaymentStatus check(String md5) {
        if (!properties.canVerify()) {
            return PaymentStatus.unverifiable();
        }

        try {
            CheckResponse body = http.post()
                    .uri("/v1/check_transaction_by_md5")
                    .header("Authorization", "Bearer " + properties.apiToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("md5", md5))
                    .retrieve()
                    .body(CheckResponse.class);

            if (body == null) {
                log.warn("Bakong returned an empty body for md5 {}", md5);
                return PaymentStatus.unknown("empty response");
            }

            // 0 means found and settled. Anything else is Bakong declining to
            // say yes, and the common case by far is "not paid yet", which is
            // not an error — it is the answer while a customer is still
            // reaching for their phone.
            if (body.responseCode() == 0 && body.data() != null) {
                return PaymentStatus.paid(
                        body.data().amount(),
                        body.data().currency(),
                        body.data().fromAccountId(),
                        body.data().hash());
            }

            return PaymentStatus.notYet(body.responseMessage());

        } catch (Exception e) {
            // A network failure is not evidence of anything. Saying "unknown"
            // keeps the order where it is; saying "not paid" would be a guess,
            // and saying "paid" would be a gift.
            log.warn("Could not reach Bakong to check {}: {}", md5, e.getMessage());
            return PaymentStatus.unknown(e.getMessage());
        }
    }

    /** What the bank said, reduced to what the till needs to decide. */
    public record PaymentStatus(
            State state,
            BigDecimal amount,
            String currency,
            String payer,
            String reference,
            String detail) {

        public enum State {
            /** Settled. Safe to close the bill. */
            PAID,
            /** Asked, and the answer was no — usually "not yet". */
            NOT_PAID,
            /** Could not ask. Nothing is known, so nothing should change. */
            UNKNOWN,
            /** No API token configured, so this can never be answered here. */
            UNVERIFIABLE
        }

        static PaymentStatus paid(BigDecimal amount, String currency, String payer, String reference) {
            return new PaymentStatus(State.PAID, amount, currency, payer, reference, null);
        }

        static PaymentStatus notYet(String detail) {
            return new PaymentStatus(State.NOT_PAID, null, null, null, null, detail);
        }

        static PaymentStatus unknown(String detail) {
            return new PaymentStatus(State.UNKNOWN, null, null, null, null, detail);
        }

        static PaymentStatus unverifiable() {
            return new PaymentStatus(State.UNVERIFIABLE, null, null, null, null,
                    "No Bakong API token configured");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CheckResponse(int responseCode, String responseMessage, Data data) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Data(String hash, String fromAccountId, String toAccountId,
                    BigDecimal amount, String currency) {
        }
    }

    @Configuration
    @EnableConfigurationProperties(KhqrProperties.class)
    static class KhqrConfig {
        @Bean
        RestClient.Builder khqrRestClientBuilder() {
            return RestClient.builder();
        }
    }
}
