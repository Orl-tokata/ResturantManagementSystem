package com.resturant.management.rms.stock;

import com.resturant.management.rms.audit.Audited;
import com.resturant.management.rms.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;

/**
 * A raw ingredient or consumable, tracked separately from {@code Product}:
 * products are what customers buy, stock items are what gets purchased and
 * consumed. "1 kg beef" is a stock item; "Lok lak beef" is a product.
 */
// qty already has its own history in stock_movement; auditing it too would
// say the same thing twice, in the noisier of the two places.
@Audited(ignore = "qty")
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

    /**
     * Which shop this belongs to.
     *
     * <p>Written by Hibernate from the signed token, never from the request,
     * and added to the WHERE clause of every query against this entity — see
     * {@code BranchTenantResolver}. Nothing in a service or repository sets or
     * reads it, which is the whole point of it being here rather than in
     * thirty-two method signatures.
     */
    @TenantId
    @Column(name = "branch_id", nullable = false)
    private Long branchId;

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
