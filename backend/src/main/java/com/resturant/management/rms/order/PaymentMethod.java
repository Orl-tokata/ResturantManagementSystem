package com.resturant.management.rms.order;

public enum PaymentMethod {
    CASH,
    CARD,
    /** ABA / Bakong KHQR scan-to-pay. */
    KHQR,
    TRANSFER
}
