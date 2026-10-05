package com.resturant.management.rms.promotion;

import com.resturant.management.rms.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * A rule that takes money off by itself.
 *
 * <p>Distinct from the discount a manager types into the payment panel: that
 * is one decision about one bill, this is a standing arrangement that applies
 * the same way to everybody who qualifies. The two are kept in separate
 * columns on the order for exactly that reason — a report that cannot tell
 * them apart cannot say whether the rule is worth running.
 */
@Entity
@Table(name = "promotion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Promotion extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Which shop's rule this is. Written and filtered by Hibernate — see V17. */
    @TenantId
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PromotionType type;

    /**
     * A percentage for PERCENT, an amount of money for AMOUNT.
     *
     * <p>The column is {@code discount_value}: ERD §3.5 says {@code value},
     * which H2 reserves, so the migration would not parse.
     */
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private PromotionScope scope;

    /** The product or category this names; null for a whole-bill rule. */
    @Column(name = "scope_id")
    private Long scopeId;

    /** Below this the rule does not apply. Null means no floor. */
    @Column(name = "min_amount", precision = 12, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    /** Happy hour. Both null means all day. */
    @Column(name = "time_from")
    private LocalTime timeFrom;

    @Column(name = "time_to")
    private LocalTime timeTo;

    /**
     * Whether the clock says this rule is running.
     *
     * <p>A window that wraps midnight — 22:00 to 02:00 — is read as "after the
     * start or before the end" rather than being refused when it was set up.
     * Late-night is a real trading pattern here and a rule that cannot express
     * it would be worked around with two rules.
     */
    public boolean appliesAt(LocalTime time) {
        if (timeFrom == null || timeTo == null) return true;
        if (timeFrom.isBefore(timeTo)) {
            return !time.isBefore(timeFrom) && !time.isAfter(timeTo);
        }
        return !time.isBefore(timeFrom) || !time.isAfter(timeTo);
    }

    /** What this rule takes off a figure, never more than the figure itself. */
    public BigDecimal discountOn(BigDecimal amount, int scale, java.math.RoundingMode rounding) {
        BigDecimal off = type == PromotionType.PERCENT
                ? amount.multiply(value).divide(BigDecimal.valueOf(100), scale, rounding)
                : value;
        return off.min(amount).setScale(scale, rounding);
    }
}
