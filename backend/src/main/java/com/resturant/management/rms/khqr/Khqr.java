package com.resturant.management.rms.khqr;

/**
 * A generated code, ready to draw.
 *
 * @param payload the string the customer's app scans
 * @param md5     how Bakong identifies this transaction when asked about it
 * @param amount  what the QR asks for, formatted as it appears in the payload
 * @param currency "USD" or "KHR"
 */
public record Khqr(String payload, String md5, String amount, String currency) {
}
