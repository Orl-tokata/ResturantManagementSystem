package com.resturant.management.rms.order;

import com.resturant.management.rms.common.BaseAuditEntity;
import com.resturant.management.rms.customer.Customer;
import com.resturant.management.rms.dining.DiningTable;
import com.resturant.management.rms.user.UserInfm;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A bill. Table {@code orders} — {@code order} is a reserved SQL word.
 *
 * <p>Totals are stored, not computed on read, so a reprinted receipt always
 * shows what was actually charged even if prices or the VAT rate change later.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseAuditEntity {

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

    @Column(name = "invoice_no", nullable = false, unique = true, length = 20)
    private String invoiceNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private DiningTable table;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id")
    private UserInfm cashier;

    /**
     * Who the bill belongs to, when anyone said. Most are null: a walk-in is
     * the ordinary case and asking everybody for a phone number at the counter
     * is how a loyalty scheme makes itself hated.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "guest_count")
    private Integer guestCount;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Builder.Default
    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    /** What a person decided to take off this one bill. */
    @Builder.Default
    @Column(name = "discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    /**
     * What the standing rules took off, kept apart from the figure above.
     *
     * <p>"The manager gave five dollars off" and "Tuesday's rule gave five
     * dollars off" are different facts, and a report that cannot separate them
     * cannot say whether the rule is worth running.
     */
    @Builder.Default
    @Column(name = "promo_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal promoDiscount = BigDecimal.ZERO;

    /** The whole-bill rule that was applied, if one was. */
    @Column(name = "promotion_id")
    private Long promotionId;

    @Builder.Default
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_khr", nullable = false, precision = 14, scale = 0)
    private BigDecimal totalKhr = BigDecimal.ZERO;

    /**
     * The rate {@code totalKhr} was worked out at.
     *
     * <p>Stamped because the setting it comes from can be corrected afterwards,
     * and a receipt someone is holding must not change when it is. The dated
     * {@code fx_rate} table is the history; this is what a reprint reads.
     */
    @Column(name = "fx_rate_khr", precision = 14, scale = 4)
    private BigDecimal fxRateKhr;

    /**
     * How the bill was settled — one row for a single tender, several for a
     * split, and a failed row for a code that was shown and abandoned.
     *
     * <p>This replaced {@code payment_method}, {@code amount_tendered} and
     * {@code change_amount}, which are still columns on the table until V13
     * drops them and which nothing reads any more (V12).
     *
     * <p>{@code BatchSize} because the history screen maps a page of twenty
     * orders at a time: without it that is twenty extra queries, with it one.
     */
    @Builder.Default
    @BatchSize(size = 50)
    @OrderBy("id ASC")
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalePayment> payments = new ArrayList<>();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.OPEN;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /* ---- KHQR ----------------------------------------------------------
       Set while a code is outstanding and kept afterwards, because a KHQR
       line in a sales report is only reconcilable against a bank statement
       if the payer and the bank's reference survive. */

    /** How Bakong identifies the transaction this code would produce. */
    @Column(name = "khqr_md5", length = 32)
    private String khqrMd5;

    /** Kept so a refreshed till redraws the same code rather than a new one. */
    @Column(name = "khqr_payload", length = 1024)
    private String khqrPayload;

    @Column(name = "khqr_expires_at")
    private LocalDateTime khqrExpiresAt;

    @Column(name = "khqr_payer", length = 120)
    private String khqrPayer;

    @Column(name = "khqr_reference", length = 120)
    private String khqrReference;

    /* ---- Helpers that keep both sides of the association in sync --------- */

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
    }

    /**
     * Empties the basket so it can be replaced.
     *
     * <p>Clearing the collection is the whole instruction: {@code orphanRemoval}
     * turns that into deletes. Detaching each line first — which this used to
     * do as well — asks Hibernate to write {@code order_id = NULL} to a column
     * that forbids it, and it does so whenever the lines are still managed from
     * an earlier save in the same persistence context. Replacing the items on a
     * bill twice inside one transaction failed on exactly that.
     */
    public void clearItems() {
        items.clear();
    }

    public void addPayment(SalePayment payment) {
        payments.add(payment);
        payment.setOrder(this);
    }

    /* ---- Derived from the payments, never stored ------------------------ */

    /** Only money that arrived. A pending or abandoned code is not payment. */
    public List<SalePayment> capturedPayments() {
        return payments.stream().filter(SalePayment::isCaptured).toList();
    }

    /** What has actually been collected against this bill. */
    public BigDecimal paidAmount() {
        return capturedPayments().stream()
                .map(SalePayment::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Whether the captured payments cover the bill — ERD §3.3's rule, and
     * what makes a split settle only once the last tender lands.
     */
    public boolean isCovered() {
        return total != null && paidAmount().compareTo(total) >= 0;
    }

    /**
     * The one method this bill was settled by, or null when it was split (or
     * not settled at all). A screen showing one method must not pick a winner
     * out of two.
     */
    public PaymentMethod singleMethod() {
        List<SalePayment> captured = capturedPayments();
        return captured.size() == 1 ? captured.get(0).getMethod() : null;
    }
}
