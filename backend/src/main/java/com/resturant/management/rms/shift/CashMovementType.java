package com.resturant.management.rms.shift;

import java.math.BigDecimal;

/**
 * What moved the cash, and which way.
 *
 * <p>The amount on a movement is always positive and the direction lives here,
 * the same arrangement {@code MovementType} uses for stock: a negative amount
 * on a PAY_OUT would silently put money into the drawer.
 */
public enum CashMovementType {

    /** A bill settled in cash. Written by the system, not by a person. */
    SALE(1, false),

    /** Cash handed back on a return. Written by the system (P7). */
    REFUND(-1, false),

    /** Money put in for a reason that is not a sale. */
    PAY_IN(1, true),

    /** Money taken out: a supplier paid at the door, a delivery tipped. */
    PAY_OUT(-1, true),

    /** More change brought to the till. */
    FLOAT(1, true),

    /** Cash removed to the safe, so the drawer does not sit on a day's takings. */
    DROP(-1, true);

    private final int sign;
    private final boolean byHand;

    CashMovementType(int sign, boolean byHand) {
        this.sign = sign;
        this.byHand = byHand;
    }

    public BigDecimal applyTo(BigDecimal amount) {
        return sign > 0 ? amount : amount.negate();
    }

    public boolean isIncrease() {
        return sign > 0;
    }

    /**
     * Whether a person may record this one.
     *
     * <p>SALE and REFUND follow from bills and would double-count if anyone
     * could also type them in; the endpoint refuses them for that reason
     * rather than trusting the screen not to offer them.
     */
    public boolean isEnteredByHand() {
        return byHand;
    }
}
