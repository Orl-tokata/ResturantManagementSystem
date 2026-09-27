package com.resturant.management.rms.khqr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two questions the rest of the application asks about KHQR.
 *
 * <p>They are deliberately different. A code can be produced as soon as there
 * is an account to pay into; knowing the money arrived additionally needs a
 * token. Conflating them would either hide a usable feature or promise a
 * confirmation that can never come.
 */
class KhqrPropertiesTest {

    private static KhqrProperties props(boolean enabled, String accountId, String token) {
        return new KhqrProperties(enabled, accountId, "Shop", "Phnom Penh", null, null,
                "POS-01", "https://api-bakong.nbc.gov.kh", token,
                Duration.ofMinutes(5), Duration.ofSeconds(8));
    }

    @Test
    @DisplayName("off by default: no account means no code, whatever else is set")
    void withoutAnAccountNothingIsOffered() {
        assertThat(props(true, null, "token").canGenerate()).isFalse();
        assertThat(props(true, "   ", "token").canGenerate()).isFalse();
        assertThat(props(false, "shop@aclb", "token").canGenerate()).isFalse();
    }

    @Test
    @DisplayName("an account is enough to show a code")
    void anAccountIsEnoughToGenerate() {
        assertThat(props(true, "shop@aclb", null).canGenerate()).isTrue();
    }

    @Test
    @DisplayName("but not enough to confirm a payment")
    void confirmingAlsoNeedsAToken() {
        // This is the case that must not be mistaken for the one above. The
        // code scans and a customer can really pay; the till simply cannot
        // learn that they did, and has to say so instead of waiting forever.
        assertThat(props(true, "shop@aclb", null).canVerify()).isFalse();
        assertThat(props(true, "shop@aclb", "   ").canVerify()).isFalse();
        assertThat(props(true, "shop@aclb", "token").canVerify()).isTrue();
    }

    @Test
    @DisplayName("nothing can be verified that cannot be generated")
    void verifyingImpliesGenerating() {
        assertThat(props(false, "shop@aclb", "token").canVerify()).isFalse();
        assertThat(props(true, null, "token").canVerify()).isFalse();
    }
}
