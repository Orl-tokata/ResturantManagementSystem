package com.resturant.management.rms.user;

/**
 * Extends the NIEI-Y4 {@code USER, ADMIN} pair to the four roles the prototype
 * needs. Existing {@code USER} rows migrate to {@link #CASHIER}.
 */
public enum Role {
    ADMIN,
    CASHIER,
    WAITER,
    CHEF
}
