package com.resturant.management.rms.purchase;

import com.resturant.management.rms.stock.StockItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_item_id", nullable = false)
    private StockItem stockItem;

    /** Snapshot of the stock item's name at purchase time. */
    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;

    @Column(name = "qty", nullable = false, precision = 12, scale = 2)
    private BigDecimal qty;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    public void recalculate() {
        if (qty != null && unitCost != null) {
            this.lineTotal = qty.multiply(unitCost);
        }
    }
}
