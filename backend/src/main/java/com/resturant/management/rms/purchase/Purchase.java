package com.resturant.management.rms.purchase;

import com.resturant.management.rms.common.BaseAuditEntity;
import com.resturant.management.rms.supplier.Supplier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Purchase order raised against a supplier. */
@Entity
@Table(name = "purchase")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Purchase extends BaseAuditEntity {

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

    @Column(name = "po_no", nullable = false, unique = true, length = 20)
    private String poNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Builder.Default
    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseItem> items = new ArrayList<>();

    @Builder.Default
    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseStatus status = PurchaseStatus.PENDING;

    @Column(name = "note", length = 500)
    private String note;

    public void addItem(PurchaseItem item) {
        items.add(item);
        item.setPurchase(this);
    }

    public void removeItem(PurchaseItem item) {
        items.remove(item);
        item.setPurchase(null);
    }
}
