package com.resturant.management.rms.khqr;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Builds a KHQR payload — the string a Bakong or ABA app reads when it scans
 * the code on the till screen.
 *
 * <p>KHQR is the National Bank of Cambodia's profile of the EMVCo merchant QR
 * standard, so this is a public format and nothing here needs a bank
 * relationship. What does need one is the account id placed in tag 29: the QR
 * is only spendable if that identifies a real Bakong account.
 *
 * <p>The format is nested TLV — each field is a two-digit tag, a two-digit
 * length, then that many characters. Length counts characters, and every value
 * KHQR permits is ASCII, so character and byte counts agree; the CRC at the end
 * is computed over the finished string.
 *
 * <p>Dynamic, not static. Tag 01 is set to "12", meaning the code carries one
 * amount and is meant to be scanned once. A static code would let a customer
 * pay any sum they liked and would match every bill for the same amount, which
 * makes reconciling a specific invoice impossible.
 *
 * @see <a href="https://bakong.nbc.gov.kh/en/download/KHQR/integration/">KHQR integration guide</a>
 */
@Component
public class KhqrGenerator {

    /* Tags used here. The full set is larger; these are the ones KHQR requires
       for a dynamic merchant payment plus the two Bakong adds. */
    private static final String TAG_PAYLOAD_FORMAT = "00";
    private static final String TAG_INITIATION_METHOD = "01";
    private static final String TAG_MERCHANT_ACCOUNT = "29";
    private static final String TAG_MERCHANT_CATEGORY = "52";
    private static final String TAG_CURRENCY = "53";
    private static final String TAG_AMOUNT = "54";
    private static final String TAG_COUNTRY = "58";
    private static final String TAG_MERCHANT_NAME = "59";
    private static final String TAG_MERCHANT_CITY = "60";
    private static final String TAG_ADDITIONAL_DATA = "62";
    private static final String TAG_TIMESTAMP = "99";
    private static final String TAG_CRC = "63";

    /** Inside tag 29. */
    private static final String SUB_BAKONG_ACCOUNT = "00";
    private static final String SUB_MERCHANT_NAME = "01";
    private static final String SUB_ACQUIRING_BANK = "02";

    /** Inside tag 62. */
    private static final String SUB_BILL_NUMBER = "01";
    private static final String SUB_REFERENCE_LABEL = "05";
    private static final String SUB_STORE_LABEL = "03";
    private static final String SUB_TERMINAL_LABEL = "07";

    /** Inside tag 99. */
    private static final String SUB_CREATED_AT = "00";

    private static final String PAYLOAD_FORMAT_VERSION = "01";
    private static final String DYNAMIC_QR = "12";
    private static final String COUNTRY_KH = "KH";

    /** ISO 4217 numeric. Bakong settles in either. */
    private static final String CURRENCY_USD = "840";
    private static final String CURRENCY_KHR = "116";

    /** ISO 18245: "Eating places and restaurants". */
    private static final String MCC_RESTAURANT = "5812";

    /** Tells apart two codes built in the same millisecond. */
    private static final SecureRandom NONCE = new SecureRandom();

    /**
     * @param amount   what to charge, in {@code currency}
     * @param currency "USD" or "KHR"
     * @param billNo   the invoice number, so a payment can be tied to a bill
     */
    public Khqr generate(KhqrMerchant merchant, BigDecimal amount, String currency, String billNo) {
        boolean khr = "KHR".equalsIgnoreCase(currency);

        // KHR has no minor unit — a QR asking for 65395.00 riel is malformed.
        String formattedAmount = khr
                ? amount.setScale(0, RoundingMode.HALF_UP).toPlainString()
                : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();

        String merchantAccount =
                tlv(SUB_BAKONG_ACCOUNT, merchant.accountId())
                        + tlv(SUB_MERCHANT_NAME, trim(merchant.name(), 25))
                        + tlv(SUB_ACQUIRING_BANK, merchant.acquiringBank());

        /*
         * A nonce, so that no two codes are ever byte-identical.
         *
         * Uniqueness used to rest on the millisecond timestamp in tag 99, and
         * two codes built inside the same millisecond came out the same — which
         * means the same md5, and Bakong identifies a transaction by md5. One
         * customer's payment would then satisfy two bills. A clock is not an
         * identifier; this is.
         */
        String nonce = "%08x".formatted(NONCE.nextInt());

        String additionalData =
                tlv(SUB_BILL_NUMBER, trim(billNo, 25))
                        + tlv(SUB_REFERENCE_LABEL, nonce)
                        + tlv(SUB_STORE_LABEL, trim(merchant.storeLabel(), 25))
                        + tlv(SUB_TERMINAL_LABEL, trim(merchant.terminalLabel(), 25));

        String timestamp = tlv(SUB_CREATED_AT, String.valueOf(System.currentTimeMillis()));

        String body =
                tlv(TAG_PAYLOAD_FORMAT, PAYLOAD_FORMAT_VERSION)
                        + tlv(TAG_INITIATION_METHOD, DYNAMIC_QR)
                        + tlv(TAG_MERCHANT_ACCOUNT, merchantAccount)
                        + tlv(TAG_MERCHANT_CATEGORY, MCC_RESTAURANT)
                        + tlv(TAG_CURRENCY, khr ? CURRENCY_KHR : CURRENCY_USD)
                        + tlv(TAG_AMOUNT, formattedAmount)
                        + tlv(TAG_COUNTRY, COUNTRY_KH)
                        + tlv(TAG_MERCHANT_NAME, trim(merchant.name(), 25))
                        + tlv(TAG_MERCHANT_CITY, trim(merchant.city(), 15))
                        + tlv(TAG_ADDITIONAL_DATA, additionalData)
                        + tlv(TAG_TIMESTAMP, timestamp);

        // The CRC covers its own tag and length as well, so they are appended
        // before the checksum is taken and the four digits follow.
        String withCrcHeader = body + TAG_CRC + "04";
        String payload = withCrcHeader + crc16(withCrcHeader);

        return new Khqr(payload, md5(payload), formattedAmount, khr ? "KHR" : "USD");
    }

    /** Tag + two-digit length + value. */
    private static String tlv(String tag, String value) {
        if (value == null || value.isEmpty()) return "";
        return tag + "%02d".formatted(value.length()) + value;
    }

    /**
     * Fields have maximum lengths, and a restaurant name can be long. Cutting
     * is better than emitting a field the scanner will reject outright.
     */
    private static String trim(String value, int max) {
        if (value == null) return null;
        String clean = value.trim();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    /**
     * CRC-16/CCITT-FALSE: polynomial 0x1021, initial value 0xFFFF, no
     * reflection, no final xor. EMVCo specifies this one; the other CRC-16
     * variants produce a checksum every scanner will reject.
     */
    static String crc16(String input) {
        int crc = 0xFFFF;
        for (byte b : input.getBytes(StandardCharsets.UTF_8)) {
            crc ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                crc = ((crc & 0x8000) != 0) ? ((crc << 1) ^ 0x1021) : (crc << 1);
                crc &= 0xFFFF;
            }
        }
        return "%04X".formatted(crc);
    }

    /**
     * Bakong identifies a transaction by the MD5 of the whole payload, which is
     * what {@code check_transaction_by_md5} takes. MD5 is a poor hash and an
     * outright bad one for security — here it is an identifier chosen by
     * someone else's API, not a defence.
     */
    static String md5(String input) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is required by every JVM", e);
        }
    }
}
