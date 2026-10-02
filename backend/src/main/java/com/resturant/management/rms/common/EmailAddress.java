package com.resturant.management.rms.common;

import java.util.Locale;
import java.util.Set;

/**
 * Whether an address can ever receive mail.
 *
 * <p>This exists because of a real afternoon: a password reset was sent to
 * {@code admin@rms.local}, the seeded placeholder, and the only sign anything
 * was wrong was a bounce arriving half an hour later in the mailbox the SMTP
 * account belongs to — not the one the person was watching. The code had been
 * generated, stored and sent correctly. It just had nowhere to go.
 *
 * <p>The names below are not a guess at what looks fake. They are reserved by
 * the standards, which means no registrar can sell them and no MX record can
 * ever exist for them:
 *
 * <ul>
 *   <li>{@code .test}, {@code .example}, {@code .invalid}, {@code .localhost}
 *       and the {@code example.*} domains — RFC 2606 and RFC 6761, set aside
 *       for documentation and testing.</li>
 *   <li>{@code .local} — RFC 6762, multicast DNS. It names machines on the
 *       network in the room, which is exactly why it is such a natural
 *       placeholder and exactly why internet mail cannot reach it.</li>
 * </ul>
 *
 * <p>Anything else is treated as deliverable. A typo, a dead domain or a
 * mailbox that has been closed are all indistinguishable from a working
 * address without actually trying, and guessing at those would start rejecting
 * real people's email.
 */
public final class EmailAddress {

    private EmailAddress() {
    }

    private static final Set<String> RESERVED_SUFFIXES = Set.of(
            ".local", ".localhost", ".test", ".example", ".invalid");

    private static final Set<String> RESERVED_DOMAINS = Set.of(
            "example.com", "example.net", "example.org", "localhost");

    /**
     * True when this address is at a domain that cannot exist on the internet.
     *
     * <p>A blank address counts: nothing can be sent to it either, and the
     * caller has the same decision to make.
     */
    public static boolean isUndeliverable(String email) {
        String domain = domainOf(email);
        if (domain.isEmpty()) return true;

        if (RESERVED_DOMAINS.contains(domain)) return true;
        return RESERVED_SUFFIXES.stream().anyMatch(domain::endsWith);
    }

    /** The part after the {@code @}, lowercased, or empty if there is not one. */
    public static String domainOf(String email) {
        if (email == null) return "";
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) return "";
        return email.substring(at + 1).trim().toLowerCase(Locale.ROOT);
    }
}
