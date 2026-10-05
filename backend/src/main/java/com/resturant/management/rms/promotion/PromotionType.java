package com.resturant.management.rms.promotion;

/**
 * How much comes off.
 *
 * <p>{@link #BUY_X_GET_Y} is in the database CHECK and refused by the service.
 * SCREENS §3.5 asked for percent and fixed first because the third interacts
 * with returns, loyalty and tax in ways that are not obvious, and a wrong
 * discount is a wrong price on a slip somebody is holding. Keeping it in the
 * constraint means adding it later is code, not a migration.
 */
public enum PromotionType {
    PERCENT,
    AMOUNT,
    BUY_X_GET_Y
}
