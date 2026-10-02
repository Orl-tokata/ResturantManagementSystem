package com.resturant.management.rms.catalog;

import com.resturant.management.rms.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A size of a dish: a large coffee beside a small one.
 *
 * <p>A distinct sellable thing with its own price, cost and barcode, which is
 * what makes it a variant and not a modifier — ARCHITECTURE §1.2. "No ice" is
 * a property of one line and lives in {@link Modifier}; expressing it here
 * would multiply the menu for something nobody stocks.
 *
 * <p>No branch of its own. A variant belongs to a product and a product
 * belongs to a branch, so the scope is one join away; what stops a variant
 * from another shop being sold is that its product cannot be loaded.
 */
@Entity
@Table(name = "product_variant")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "sku", length = 50)
    private String sku;

    /** Expected to be empty for food; packaged drinks have one. */
    @Column(name = "barcode", length = 50)
    private String barcode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    /** What this size sells for. Not a delta: a size is priced, not adjusted. */
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @Column(name = "cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
