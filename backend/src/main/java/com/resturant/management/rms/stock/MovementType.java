package com.resturant.management.rms.stock;

import java.math.BigDecimal;

/**
 * Why stock moved, and which way.
 *
 * <p>The direction lives here rather than in the sign of the quantity, which
 * stays positive. That was the existing convention and re-signing every row
 * would have rewritten history to mean what it already meant — docs/ERD.md
 * §1.4.
 */
public enum MovementType {
    /** Received from a purchase, or a manual increase. */
    IN(1),
    /** A manual decrease. */
    OUT(-1),
    /** Spoiled, broken or lost. */
    DAMAGED(-1),
    /** Left as part of a settled bill. */
    SALE(-1),
    /** Came back from one. */
    RETURN(1);

    private final int sign;

    MovementType(int sign) {
        this.sign = sign;
    }

    /** The quantity as it applies to a balance: positive adds, negative removes. */
    public BigDecimal applyTo(BigDecimal qty) {
        return sign > 0 ? qty : qty.negate();
    }

    public boolean isIncrease() {
        return sign > 0;
    }
}
