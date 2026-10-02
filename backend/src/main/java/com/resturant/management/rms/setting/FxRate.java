package com.resturant.management.rms.setting;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * What a currency was worth, from a given day.
 *
 * <p>The riel rate used to be one editable setting, so changing it changed the
 * past: a report run today converted last month's sales at today's figure. One
 * row per change means "what were we converting at in March" has an answer.
 *
 * <p>This is the history. What a reprinted receipt reads is the rate stamped
 * on the order itself, because a rate corrected after the fact must not rewrite
 * a slip the customer is holding.
 */
@Entity
@Table(name = "fx_rate")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ISO code. Only KHR today; the column exists so a second one needs no migration. */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** How many of this currency one US dollar buys. */
    @Column(name = "rate", nullable = false, precision = 14, scale = 4)
    private BigDecimal rate;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
