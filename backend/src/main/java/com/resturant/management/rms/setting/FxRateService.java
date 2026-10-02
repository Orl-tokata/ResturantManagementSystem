package com.resturant.management.rms.setting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * The record of what the riel was worth, and when.
 *
 * <p>Writing is driven by the settings screen rather than by a separate editor:
 * an owner changes the rate in one place, and this keeps the history of those
 * changes so that a report covering March can say what March was converted at.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FxRateService {

    public static final String KHR = "KHR";

    private final FxRateRepository repository;

    @Transactional(readOnly = true)
    public List<FxRate> history(String currency, int limit) {
        return repository.findByCurrencyOrderByValidFromDescIdDesc(currency, Limit.of(limit));
    }

    /**
     * Records a rate change, dated today.
     *
     * <p>One row per day, not per save: an owner who corrects a typo three
     * times before lunch has changed the rate once as far as any report is
     * concerned, and three rows for one day would make {@code valid_from}
     * ambiguous. The last save of the day wins, which is the one the tills
     * actually used for most of it.
     *
     * <p>A rate that has not moved writes nothing. A history whose rows all say
     * the same number is a log of saves, not of rates.
     */
    @Transactional
    public void record(String currency, BigDecimal rate, String by) {
        if (rate == null || rate.signum() <= 0) return;

        LocalDate today = LocalDate.now();
        FxRate existing = repository.findByCurrencyAndValidFrom(currency, today).orElse(null);

        if (existing != null) {
            if (existing.getRate().compareTo(rate) == 0) return;
            existing.setRate(rate);
            existing.setCreatedBy(by);
            existing.setCreatedAt(LocalDateTime.now());
            repository.save(existing);
            log.info("Rate for {} corrected to {} for today", currency, rate);
            return;
        }

        BigDecimal previous = repository
                .findFirstByCurrencyAndValidFromLessThanEqualOrderByValidFromDescIdDesc(currency, today)
                .map(FxRate::getRate)
                .orElse(null);
        if (previous != null && previous.compareTo(rate) == 0) return;

        repository.save(FxRate.builder()
                .currency(currency)
                .rate(rate)
                .validFrom(today)
                .createdBy(by)
                .createdAt(LocalDateTime.now())
                .build());
        log.info("Rate for {} is now {} (was {})", currency, rate, previous);
    }
}
