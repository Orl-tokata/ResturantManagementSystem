package com.resturant.management.rms.common;

/** Small string helpers shared by the search endpoints. */
public final class Strings {

    private Strings() {
    }

    /**
     * Normalises an optional search term.
     *
     * <p>The repository queries treat {@code null} as "no filter", so an empty
     * or whitespace-only parameter must become null rather than matching
     * everything with {@code LIKE '%%'}.
     */
    public static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
