package com.resturant.management.rms.khqr;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Bakong KHQR settings.
 *
 * <p>Two of these carry weight and the rest are presentation. {@code accountId}
 * decides whose account the money lands in — a QR built with the wrong one is
 * perfectly valid and pays somebody else. {@code apiToken} is what lets this
 * application ask Bakong whether a payment actually arrived; without it the
 * code can still be shown and scanned, but nothing can confirm settlement, so
 * an order will not move to PAID on its own.
 *
 * @param enabled       whether the till offers KHQR at all
 * @param accountId     Bakong account, e.g. {@code somebody@aclb}
 * @param merchantName  shown in the payer's app; falls back to the restaurant
 *                      name in settings when blank
 * @param city          required by the format
 * @param acquiringBank optional, names the bank behind the account
 * @param storeLabel    optional, which branch
 * @param terminalLabel optional, which till
 * @param apiBaseUrl    Bakong Open API root
 * @param apiToken      bearer token from Bakong's developer portal
 * @param expiry        how long a code stays payable before the till abandons
 *                      it. Short on purpose: a code left open is a bill that
 *                      cannot be settled any other way while it waits.
 * @param requestTimeout how long to wait on Bakong before giving up on one check
 */
@ConfigurationProperties(prefix = "app.khqr")
public record KhqrProperties(
        @DefaultValue("false") boolean enabled,
        String accountId,
        String merchantName,
        @DefaultValue("Phnom Penh") String city,
        String acquiringBank,
        String storeLabel,
        @DefaultValue("POS-01") String terminalLabel,
        @DefaultValue("https://api-bakong.nbc.gov.kh") String apiBaseUrl,
        String apiToken,
        @DefaultValue("5m") Duration expiry,
        @DefaultValue("8s") Duration requestTimeout) {

    /** A code can be produced as soon as there is an account to pay into. */
    public boolean canGenerate() {
        return enabled && accountId != null && !accountId.isBlank();
    }

    /**
     * Whether settlement can be confirmed automatically.
     *
     * <p>When this is false the code still works — a customer can scan and pay
     * — but nothing here can tell that they did, so the till must not claim the
     * bill is settled.
     */
    public boolean canVerify() {
        return canGenerate() && apiToken != null && !apiToken.isBlank();
    }
}
