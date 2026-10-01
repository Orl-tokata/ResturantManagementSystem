package com.resturant.management.rms.catalog;

import com.resturant.management.rms.audit.Audited;
import com.resturant.management.rms.common.BaseAuditEntity;
import com.resturant.management.rms.common.RecordStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

// stockQty moves on every sale line — it belongs in the movement ledger
// docs/PLAN.md P4 introduces, not in a log kept for price and menu edits.
@Audited(ignore = "stockQty")
@Entity
@Table(name = "product")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "name_en", length = 150)
    private String nameEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** Selling price. */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** Cost price, used for the profit column on reports. */
    @Builder.Default
    @Column(name = "cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "stock_qty", nullable = false, precision = 12, scale = 2)
    private BigDecimal stockQty = BigDecimal.ZERO;

    /** Emoji in seed data; a URL once real uploads land. */
    /** An emoji, not a URL. See V9 — the column was misnamed for both. */
    @Column(name = "icon", length = 16)
    private String icon;

    @Column(name = "description", length = 1000)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;
}
