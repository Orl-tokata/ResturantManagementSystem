package com.resturant.management.rms.customer;

import com.resturant.management.rms.order.Order;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One movement of points.
 *
 * <p>{@code points} is signed, unlike {@code stock_movement} where the
 * direction lives in the type. The inconsistency is admitted in ERD §3.4:
 * stock's convention is established in a populated table and churning it would
 * be a migration for tidiness. New tables get signed amounts.
 */
@Entity
@Table(name = "loyalty_transaction")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** The bill behind it, or null for an adjustment made by hand. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private LoyaltyType type;

    /**
     * The bill's id on an EARN row, null on every other kind.
     *
     * <p>Not a duplicate of {@code order} for its own sake: it is the
     * one-earn-per-bill rule. A unique index on it allows many nulls and no
     * two equal values, which is "a bill earns once and may be returned as
     * often as it has lines" expressed portably — see V16, and V14 for the
     * same shape.
     */
    @Column(name = "earn_order_id")
    private Long earnOrderId;

    /** Positive adds, negative takes away. */
    @Column(name = "points", nullable = false, precision = 12, scale = 2)
    private BigDecimal points;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
