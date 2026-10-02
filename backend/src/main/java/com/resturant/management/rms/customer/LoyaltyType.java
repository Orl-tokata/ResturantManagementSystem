package com.resturant.management.rms.customer;

/**
 * Why points moved.
 *
 * <p>Only EARN and ADJUST are written in Phase 1: points are given by settling
 * a bill and corrected by a manager. REDEEM arrives with a way to spend them,
 * REVERSE with returns (P7), and EXPIRE with a policy that says when — none of
 * which exists yet. They are in the CHECK so that adding them later is code
 * rather than a migration.
 */
public enum LoyaltyType {
    EARN,
    REDEEM,
    ADJUST,
    EXPIRE,
    REVERSE
}
