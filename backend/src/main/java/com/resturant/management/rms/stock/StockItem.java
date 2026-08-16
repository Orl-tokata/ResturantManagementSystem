package com.resturant.management.rms.stock;

import com.resturant.management.rms.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A raw ingredient or consumable, tracked separately from {@code Product}:
 * products are what customers buy, stock items are what gets purchased and
 * consumed. "1 kg beef" is a stock item; "Lok lak beef" is a product.
 */
@Entity
@Table(name = "stock_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItem extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /** Khmer unit label, e.g. គីឡូក្រាម, ដប, កំប៉ុង. */
    @Column(name = "unit", nullable = false, length = 30)
    private String unit;

    @Builder.Default
    @Column(name = "qty", nullable = false, precision = 12, scale = 2)
    private BigDecimal qty = BigDecimal.ZERO;

    /** Reorder threshold — drives the low-stock badge. */
    @Builder.Default
    @Column(name = "min_qty", nullable = false, precision = 12, scale = 2)
    private BigDecimal minQty = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    public boolean isLowStock() {
        return qty != null && minQty != null && qty.compareTo(minQty) < 0;
    }

    public boolean isOutOfStock() {
        return qty != null && qty.compareTo(BigDecimal.ZERO) <= 0;
    }
}
