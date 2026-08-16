package com.resturant.management.rms.order;

import com.resturant.management.rms.catalog.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "note", length = 255)
    private String note;

    /** Recomputes {@code lineTotal} from qty × unitPrice. */
    public void recalculate() {
        if (qty != null && unitPrice != null) {
            this.lineTotal = qty.multiply(unitPrice);
        }
    }
}
