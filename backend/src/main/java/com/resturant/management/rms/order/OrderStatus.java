package com.resturant.management.rms.order;

public enum OrderStatus {
    /** Bill is open at a table and still editable. */
    OPEN,

    /**
     * A KHQR code has been shown and the till is waiting for Bakong to confirm
     * the money arrived.
     *
     * <p>Deliberately not PAID. The distinction is the whole point of the
     * integration: a code on a screen proves a customer was asked, not that
     * they paid. An order here is frozen — it cannot be edited, and it cannot
     * be settled by another method until the code expires or is abandoned.
     */
    AWAITING_PAYMENT,

    PAID,
    CANCELLED
}
