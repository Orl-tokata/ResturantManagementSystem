package com.resturant.management.rms.catalog;

import com.resturant.management.rms.audit.Audited;
import com.resturant.management.rms.common.BaseAuditEntity;
import com.resturant.management.rms.common.RecordStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Stored photograph, as a bare filename — the directory is configuration.
     *
     * <p>Null is the ordinary case, and the icon above is what the screens show
     * then. See V10.
     */
    @Column(name = "image_file", length = 80)
    private String imageFile;

    @Column(name = "description", length = 1000)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;

    /**
     * Sizes of this dish. Empty for most of the menu, which is the ordinary
     * case: a plate of fried rice is one thing.
     *
     * <p>Inside the product rather than on a screen of their own — SCREENS §4
     * is explicit that a separate variant manager means two places to look for
     * one product's price.
     */
    @Builder.Default
    @OrderBy("sortOrder ASC, id ASC")
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    /**
     * The questions asked when this is ordered.
     *
     * <p>A link, not ownership: the same "sugar level" group is attached to
     * every drink that asks it.
     */
    @Builder.Default
    @ManyToMany
    @JoinTable(name = "product_modifier_group",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    @OrderBy("sortOrder ASC, id ASC")
    private List<ModifierGroup> modifierGroups = new ArrayList<>();

    public void addVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this);
    }
}
