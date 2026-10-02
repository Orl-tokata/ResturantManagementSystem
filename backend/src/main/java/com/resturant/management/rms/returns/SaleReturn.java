package com.resturant.management.rms.returns;

import com.resturant.management.rms.common.BaseAuditEntity;
import com.resturant.management.rms.order.Order;
import com.resturant.management.rms.order.PaymentMethod;
import com.resturant.management.rms.shift.CashShift;
import com.resturant.management.rms.user.UserInfm;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Money going back, as its own document.
 *
 * <p>It points at the original sale and never touches it. A settled bill is a
 * printed record somebody may still be holding; editing it to show what was
 * given back would leave the slip in their hand and the row in the database
 * disagreeing, with nothing to say which came first.
 */
@Entity
@Table(name = "sale_return")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturn extends BaseAuditEntity {

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

    @Column(name = "return_no", nullable = false, unique = true, length = 20)
    private String returnNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** The drawer the cash came out of, or null when none did. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private CashShift shift;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", nullable = false, length = 20)
    private PaymentMethod refundMethod;

    /** Set only when the amount required somebody senior to agree. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private UserInfm approvedBy;

    @Builder.Default
    @OneToMany(mappedBy = "saleReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleReturnItem> items = new ArrayList<>();

    public void addItem(SaleReturnItem item) {
        items.add(item);
        item.setSaleReturn(this);
    }
}
