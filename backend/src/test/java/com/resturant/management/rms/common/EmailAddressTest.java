package com.resturant.management.rms.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Which addresses can never receive mail.
 *
 * <p>The line matters in both directions. Too narrow and a reset code goes
 * silently into a void, which is the bug this was written for. Too wide and
 * the system starts refusing to email real customers because their domain
 * looked unfamiliar.
 */
class EmailAddressTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "admin@rms.local",          // the seeded placeholder that started this
            "someone@printer.local",
            "root@localhost",
            "a@b.localhost",
            "test@example.com",
            "test@EXAMPLE.ORG",         // case is not a disguise
            "qa@anything.test",
            "nobody@nowhere.invalid",
            "docs@my.example",
    })
    @DisplayName("reserved domains can never have a mail server")
    void reserved(String email) {
        assertThat(EmailAddress.isUndeliverable(email)).isTrue();
    }

    /**
     * Everything else is treated as deliverable. A dead domain and a live one
     * are indistinguishable without trying, and guessing would reject real
     * people.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "orltokata@gmail.com",
            "owner@angkor-restaurant.com.kh",
            "someone@localhost.example.co.uk",   // ends in a real TLD
            "a@rms.locally",                     // not .local
            "b@notexample.com",
    })
    @DisplayName("an ordinary address is left alone")
    void ordinary(String email) {
        assertThat(EmailAddress.isUndeliverable(email)).isFalse();
    }

    @Test
    @DisplayName("nothing at all counts as undeliverable, because nothing can be sent to it")
    void missing() {
        assertThat(EmailAddress.isUndeliverable(null)).isTrue();
        assertThat(EmailAddress.isUndeliverable("")).isTrue();
        assertThat(EmailAddress.isUndeliverable("no-at-sign")).isTrue();
        assertThat(EmailAddress.isUndeliverable("trailing@")).isTrue();
    }

    @Test
    @DisplayName("the domain is whatever follows the last @")
    void domain() {
        assertThat(EmailAddress.domainOf("a@b.com")).isEqualTo("b.com");
        assertThat(EmailAddress.domainOf("odd@name@b.com")).isEqualTo("b.com");
        assertThat(EmailAddress.domainOf("  a@B.CoM  ")).isEqualTo("b.com");
        assertThat(EmailAddress.domainOf(null)).isEmpty();
    }
}
