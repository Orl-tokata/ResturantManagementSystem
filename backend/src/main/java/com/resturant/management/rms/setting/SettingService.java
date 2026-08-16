package com.resturant.management.rms.setting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Typed access to {@code app_setting}.
 *
 * <p>Every getter takes a fallback: a missing or malformed row must not stop the
 * tills from ringing up sales, so a bad value is logged and the default used.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingService {

    public static final String VAT_RATE = "sales.vatRate";
    public static final String KHR_RATE = "currency.khrRate";
    public static final String INVOICE_PREFIX = "sales.invoicePrefix";
    public static final String PURCHASE_PREFIX = "sales.purchasePrefix";
    public static final String ALLOW_DISCOUNT = "option.allowDiscount";
    public static final String REQUIRE_TABLE = "option.requireTable";

    private final AppSettingRepository repository;

    @Transactional(readOnly = true)
    public String getString(String key, String fallback) {
        return repository.findBySettingKey(key)
                .map(AppSetting::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(fallback);
    }

    @Transactional(readOnly = true)
    public BigDecimal getDecimal(String key, BigDecimal fallback) {
        String raw = getString(key, null);
        if (raw == null) return fallback;
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            log.warn("Setting '{}' is not a number ('{}') — using {}", key, raw, fallback);
            return fallback;
        }
    }

    @Transactional(readOnly = true)
    public boolean getBoolean(String key, boolean fallback) {
        String raw = getString(key, null);
        return raw == null ? fallback : Boolean.parseBoolean(raw.trim());
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAll() {
        return repository.findAll().stream()
                .collect(Collectors.toMap(
                        AppSetting::getSettingKey,
                        s -> s.getSettingValue() == null ? "" : s.getSettingValue(),
                        (a, b) -> a));
    }

    @Transactional
    public void put(String key, String value) {
        AppSetting setting = repository.findBySettingKey(key)
                .orElseGet(() -> AppSetting.builder().settingKey(key).build());
        setting.setSettingValue(value);
        repository.save(setting);
    }

    @Transactional
    public void putAll(Map<String, String> values) {
        values.forEach(this::put);
    }

    @Transactional(readOnly = true)
    public List<AppSetting> byPrefix(String prefix) {
        return repository.findBySettingKeyStartingWith(prefix);
    }

    /** Convenience wrappers used by the order and purchase flows. */

    public BigDecimal vatRate() {
        return getDecimal(VAT_RATE, BigDecimal.TEN);
    }

    public BigDecimal khrRate() {
        return getDecimal(KHR_RATE, new BigDecimal("4100"));
    }

    public String invoicePrefix() {
        return getString(INVOICE_PREFIX, "INV-");
    }

    public String purchasePrefix() {
        return getString(PURCHASE_PREFIX, "PO-");
    }
}
