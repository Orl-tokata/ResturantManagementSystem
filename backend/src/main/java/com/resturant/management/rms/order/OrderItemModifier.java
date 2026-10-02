package com.resturant.management.rms.order;

import com.resturant.management.rms.catalog.Modifier;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One thing asked for on one line: "no ice", "extra spicy".
 *
 * <p>The name and the price are copied here, for the same reason
 * {@code productName} and {@code unitPrice} already are on the line itself: a
 * receipt has to stay true after a rename or a reprice. {@code modifier} is
 * nullable so taking something off the menu does not destroy the record of the
 * people who ordered it.
 */
@Entity
@Table(name = "order_item_modifier")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifier_id")
    private Modifier modifier;

    @Column(name = "modifier_name", nullable = false, length = 100)
    private String modifierName;

    /** Signed, as it was when this was ordered. */
    @Builder.Default
    @Column(name = "price_delta", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceDelta = BigDecimal.ZERO;
}
