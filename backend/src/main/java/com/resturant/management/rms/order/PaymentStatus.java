package com.resturant.management.rms.order;

/**
 * What became of one attempt to collect money.
 *
 * <p>Only {@link #CAPTURED} counts towards a bill being paid. The others exist
 * because an attempt that produced nothing is still something that happened: a
 * code shown and abandoned used to leave no trace at all, which made "we showed
 * three QR codes and took one payment" unanswerable.
 */
public enum PaymentStatus {

    /** A KHQR code is on screen. The bank has not said anything yet. */
    PENDING,

    /** The money arrived. */
    CAPTURED,

    /** The attempt ended without money — an abandoned or expired code. */
    FAILED,

    /** Reversed by a return. Written by P7; nothing produces it yet. */
    REFUNDED
}
