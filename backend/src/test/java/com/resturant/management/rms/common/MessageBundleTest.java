package com.resturant.management.rms.common;

import com.resturant.management.rms.common.i18n.Messages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the message bundle, because the compiler cannot.
 *
 * <p>A message key is a string. A typo in one, or a key added to English and
 * forgotten in Khmer, compiles and passes every other test — it only shows up
 * as a raw {@code error.order.emptyBill} on a cashier's screen. These four
 * checks are what turn that into a build failure.
 */
@SpringBootTest
class MessageBundleTest {

    private static final Locale KHMER = Locale.forLanguageTag("km");

    @Autowired Messages messages;
    @Autowired com.resturant.management.rms.common.exception.GlobalExceptionHandler handler;

    private Properties load(String locale) throws IOException {
        Properties p = new Properties();
        try (InputStream in = getClass().getResourceAsStream(
                "/messages/messages_" + locale + ".properties")) {
            assertThat(in).as("messages_%s.properties on the classpath", locale).isNotNull();
            p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return p;
    }

    @Test
    @DisplayName("both bundles define exactly the same keys")
    void bundlesAgree() throws IOException {
        Set<String> en = new LinkedHashSet<>(load("en").stringPropertyNames());
        Set<String> km = new LinkedHashSet<>(load("km").stringPropertyNames());

        Set<String> missingInKm = new LinkedHashSet<>(en);
        missingInKm.removeAll(km);
        Set<String> missingInEn = new LinkedHashSet<>(km);
        missingInEn.removeAll(en);

        assertThat(missingInKm).as("keys defined in English but not Khmer").isEmpty();
        assertThat(missingInEn).as("keys defined in Khmer but not English").isEmpty();
        assertThat(en).isNotEmpty();
    }

    /**
     * Every value is a MessageFormat pattern, so a single quote that should
     * have been doubled silently swallows its neighbours — {@code '{0}'} comes
     * out as the literal {@code {0}} rather than the argument. Rendering with
     * enough dummy arguments and then looking for a leftover brace catches it.
     */
    @Test
    @DisplayName("no pattern leaves an unsubstituted placeholder")
    void patternsAreWellQuoted() throws IOException {
        Object[] dummies = {"A", "B", "C", "D", "E", "F"};
        List<String> broken = new ArrayList<>();

        for (String locale : List.of("en", "km")) {
            Locale l = locale.equals("en") ? Locale.ENGLISH : KHMER;
            Properties p = load(locale);
            for (String key : p.stringPropertyNames()) {
                String rendered = messages.get(l, key, dummies);
                if (rendered.contains("{") || rendered.contains("}")) {
                    broken.add(locale + " / " + key + " -> " + rendered);
                }
            }
        }

        assertThat(broken)
                .as("patterns that still contain a brace after formatting; usually a "
                        + "single quote that needed doubling")
                .isEmpty();
    }

    /**
     * Scans the production sources for message-key literals and asserts each
     * one resolves. This is the check that catches a typo at the throw site,
     * which is otherwise invisible until that error actually fires.
     */
    @Test
    @DisplayName("every key referenced in Java exists in the bundle")
    void referencedKeysExist() throws IOException {
        Set<String> defined = load("en").stringPropertyNames();
        Path sources = Path.of("src/main/java");
        assertThat(Files.isDirectory(sources)).as("source tree is where the test expects").isTrue();

        // "error.x.y", "valid.x", "{valid.x}" in an annotation, "entity.x",
        // "field.x", "usedBy.x", "setting.x" — the prefixes the bundle owns.
        Pattern literal = Pattern.compile(
                "\"\\{?((?:error|valid|entity|field|usedBy|setting)\\.[A-Za-z0-9_.]+)\\}?\"");

        List<String> unknown = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sources)) {
            for (Path f : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String src = Files.readString(f, StandardCharsets.UTF_8);
                Matcher m = literal.matcher(src);
                while (m.find()) {
                    String key = m.group(1);
                    // "field." + e.getField() is built at runtime and is
                    // deliberately allowed to miss; the handler falls back.
                    if (!defined.contains(key)) {
                        unknown.add(f.getFileName() + ": " + key);
                    }
                }
            }
        }

        assertThat(unknown).as("message keys used in code but absent from the bundle").isEmpty();
    }

    @Test
    @DisplayName("the same key resolves to different text per locale")
    void localesActuallyDiffer() {
        String en = messages.get(Locale.ENGLISH, "error.order.emptyBill");
        String km = messages.get(KHMER, "error.order.emptyBill");

        assertThat(en).isEqualTo("Cannot take payment for an empty bill");
        assertThat(km).isNotEqualTo(en);
        // Khmer script starts at U+1780; proves the file was read as UTF-8 and
        // not mangled into ISO-8859-1 on the way in.
        assertThat(km.chars().anyMatch(c -> c >= 0x1780 && c <= 0x17FF)).isTrue();
    }

    /**
     * The handler stringifies arguments before formatting. Without that,
     * MessageFormat runs them through the locale NumberFormat: an id comes back
     * grouped ("999.999" under km) and a BigDecimal loses its trailing zero, so
     * an error message about an amount reports a different amount.
     */
    @Test
    @DisplayName("numbers are not locale-formatted: no grouping, no lost scale")
    void numbersPassThroughUnformatted() {
        var response = handler.handleApi(new com.resturant.management.rms.common.exception
                .BadRequestException("error.stock.insufficient",
                new java.math.BigDecimal("12.50"), "Beef", new java.math.BigDecimal("3.00")));

        String message = response.getBody().getMessage();
        assertThat(message).contains("12.50").contains("3.00");

        var notFound = handler.handleApi(
                com.resturant.management.rms.common.exception.NotFoundException.of("entity.category", 999999L));
        assertThat(notFound.getBody().getMessage()).contains("999999");
    }

    @Test
    @DisplayName("arguments are substituted, including a nested entity name")
    void argumentsSubstitute() {
        assertThat(messages.get(Locale.ENGLISH, "error.notFound", "Category", 5L))
                .isEqualTo("Category not found: 5");
        assertThat(messages.get(Locale.ENGLISH, "error.duplicate", "Staff", "code", "EMP-001"))
                .isEqualTo("Staff with code 'EMP-001' already exists");
    }
}
