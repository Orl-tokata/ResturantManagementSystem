package com.resturant.management.rms.order;

import com.resturant.management.rms.catalog.Product;
import com.resturant.management.rms.catalog.ProductVariant;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A single line on a bill.
 *
 * <p>{@code productName} and {@code unitPrice} are copied from the product at
 * the moment of ordering. That denormalisation is deliberate: renaming or
 * repricing a product must not rewrite history on already-printed receipts,
 * and {@code product} is nullable so a deleted product does not orphan the line.
 */
@Entity
@Table(name = "order_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "qty", nullable = false, precision = 12, scale = 2)
    private BigDecimal qty;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /**
     * What the dish cost us, copied at the moment of ordering for the same
     * reason {@code unitPrice} is: the margin on a sale is the margin that was
     * made, and repricing an ingredient next month must not rewrite it.
     */
    @Builder.Default
    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    /**
     * True when {@code unitCost} was filled in afterwards rather than recorded
     * at the time \u2014 every line sold before V13, which got today's cost
     * because the real one was never written down. The reports label those
     * margins estimated instead of presenting them as measured.
     */
    @Builder.Default
    @Column(name = "cost_estimated", nullable = false)
    private boolean costEstimated = false;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    /**
     * The size that was sold, when the dish has sizes.
     *
     * <p>{@code unitPrice} already records what was charged, so the money is
     * safe without this. What it adds is which of them it was: otherwise a
     * reprint says "Coffee" for a large one and a report cannot tell the two
     * apart. The name is copied for the same reason the product's is.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @Column(name = "variant_name", length = 100)
    private String variantName;

    /**
     * What was asked for on this line.
     *
     * <p>Their price deltas are already inside {@code unitPrice} — they are
     * part of what the line cost, not an adjustment applied afterwards — so
     * these rows are the record of what was asked, not a second sum.
     */
    @Builder.Default
    @OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemModifier> modifiers = new ArrayList<>();

    @Column(name = "note", length = 255)
    private String note;

    public void addModifier(OrderItemModifier modifier) {
        modifiers.add(modifier);
        modifier.setOrderItem(this);
    }

    /** Recomputes {@code lineTotal} from qty × unitPrice. */
    public void recalculate() {
        if (qty != null && unitPrice != null) {
            this.lineTotal = qty.multiply(unitPrice);
        }
    }
}
