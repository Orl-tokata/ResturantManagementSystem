package com.resturant.management.rms.user;

/**
 * Extends the NIEI-Y4 {@code USER, ADMIN} pair to the four roles the prototype
 * needs. Existing {@code USER} rows migrate to {@link #CASHIER}.
 */
public enum Role {
    ADMIN,
    /**
     * Added by V17. {@code @PreAuthorize} has referred to it since P3 and the
     * role CHECK refused it until then, so it existed in the application's
     * vocabulary and nowhere else.
     */
    MANAGER,
    CASHIER,
    WAITER,
    CHEF
}
