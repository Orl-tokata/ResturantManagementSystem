package com.resturant.management.rms.khqr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The QR is the one half of Bakong payment that can be proved without a bank.
 *
 * <p>A malformed payload fails in the worst possible way: the code appears on
 * the screen, the customer holds a phone up to it, and nothing happens. So the
 * structure is checked field by field here rather than by looking at a picture
 * of it.
 */
class KhqrGeneratorTest {

    private final KhqrGenerator generator = new KhqrGenerator();

    private final KhqrMerchant merchant = new KhqrMerchant(
            "angkor_restaurant@aclb",
            "Angkor Restaurant",
            "Phnom Penh",
            "ACLEDA Bank",
            "Main",
            "POS-01");

    /** Walks the TLV string into a map of tag -> value. */
    private static Map<String, String> parse(String payload) {
        Map<String, String> fields = new LinkedHashMap<>();
        int i = 0;
        while (i + 4 <= payload.length()) {
            String tag = payload.substring(i, i + 2);
            int length = Integer.parseInt(payload.substring(i + 2, i + 4));
            fields.put(tag, payload.substring(i + 4, i + 4 + length));
            i += 4 + length;
        }
        return fields;
    }

    @Test
    @DisplayName("CRC-16/CCITT-FALSE matches the published check value")
    void crcMatchesTheStandardCheckValue() {
        // Every CRC-16 variant agrees on the name and disagrees on the answer.
        // "123456789" is the standard check string; CCITT-FALSE gives 0x29B1,
        // where CRC-16/XMODEM gives 0x31C3 and ARC gives 0xBB3D. Getting this
        // wrong produces a QR that scans and is then rejected.
        assertThat(KhqrGenerator.crc16("123456789")).isEqualTo("29B1");
    }

    @Test
    @DisplayName("the payload is well-formed TLV that parses back to what went in")
    void payloadIsWellFormed() {
        Khqr qr = generator.generate(merchant, new BigDecimal("15.95"), "USD", "INV-00183");
        Map<String, String> f = parse(qr.payload());

        assertThat(f.get("00")).isEqualTo("01");              // payload format
        assertThat(f.get("01")).isEqualTo("12");              // dynamic: one amount, scanned once
        assertThat(f.get("52")).isEqualTo("5812");            // restaurant
        assertThat(f.get("53")).isEqualTo("840");             // USD
        assertThat(f.get("54")).isEqualTo("15.95");
        assertThat(f.get("58")).isEqualTo("KH");
        assertThat(f.get("59")).isEqualTo("Angkor Restaurant");
        assertThat(f.get("60")).isEqualTo("Phnom Penh");

        // The account the money actually goes to.
        assertThat(parse(f.get("29")).get("00")).isEqualTo("angkor_restaurant@aclb");
        // The bill it belongs to, which is what makes reconciliation possible.
        assertThat(parse(f.get("62")).get("01")).isEqualTo("INV-00183");
    }

    @Test
    @DisplayName("the CRC at the end covers the whole payload including its own header")
    void crcCoversEverythingBeforeIt() {
        Khqr qr = generator.generate(merchant, new BigDecimal("9.90"), "USD", "INV-1");
        String payload = qr.payload();

        assertThat(payload).contains("6304");
        String body = payload.substring(0, payload.length() - 4);
        String checksum = payload.substring(payload.length() - 4);

        assertThat(body).endsWith("6304");
        assertThat(checksum).isEqualTo(KhqrGenerator.crc16(body));
        // Four uppercase hex digits, not lowercase and not truncated.
        assertThat(checksum).matches("[0-9A-F]{4}");
    }

    @Test
    @DisplayName("riel is whole units — a QR asking for 65395.00 riel is malformed")
    void khrHasNoMinorUnit() {
        Khqr qr = generator.generate(merchant, new BigDecimal("65395.00"), "KHR", "INV-2");
        Map<String, String> f = parse(qr.payload());

        assertThat(f.get("53")).isEqualTo("116");
        assertThat(f.get("54")).isEqualTo("65395");
        assertThat(qr.currency()).isEqualTo("KHR");
    }

    @Test
    @DisplayName("dollars always carry two decimals, even when round")
    void usdKeepsItsMinorUnit() {
        Khqr qr = generator.generate(merchant, new BigDecimal("9"), "USD", "INV-3");
        assertThat(parse(qr.payload()).get("54")).isEqualTo("9.00");
    }

    @Test
    @DisplayName("an over-long name is cut rather than emitted at an illegal length")
    void longFieldsAreTrimmed() {
        KhqrMerchant longName = new KhqrMerchant(
                "x@aclb",
                "A Restaurant With A Name Far Longer Than The Format Permits",
                "Phnom Penh", null, null, null);

        Map<String, String> f = parse(generator.generate(longName, BigDecimal.ONE, "USD", "INV-4").payload());
        assertThat(f.get("59")).hasSize(25);
    }

    @Test
    @DisplayName("each code is distinct, so two identical bills are not one payment")
    void eachCodeIsUnique() {
        // Same amount, same bill number, generated twice: the timestamp differs,
        // so the md5 does too. Without that, a second customer paying the same
        // total would look to Bakong like the first customer's transaction.
        Khqr a = generator.generate(merchant, new BigDecimal("5.00"), "USD", "INV-5");
        Khqr b = generator.generate(merchant, new BigDecimal("5.00"), "USD", "INV-5");

        assertThat(a.md5()).isNotEqualTo(b.md5());
        assertThat(a.md5()).matches("[0-9a-f]{32}");
    }

    @Test
    @DisplayName("an empty optional field is omitted, not written as a zero-length tag")
    void emptyFieldsAreOmitted() {
        KhqrMerchant bare = new KhqrMerchant("x@aclb", "Shop", "Phnom Penh", null, null, null);
        Map<String, String> f = parse(generator.generate(bare, BigDecimal.ONE, "USD", "INV-6").payload());

        assertThat(parse(f.get("29"))).doesNotContainKey("02");
        assertThat(parse(f.get("62"))).containsKey("01").doesNotContainKey("03");
    }
}
