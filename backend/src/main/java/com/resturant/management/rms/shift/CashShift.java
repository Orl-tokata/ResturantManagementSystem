package com.resturant.management.rms.shift;

import com.resturant.management.rms.common.BaseAuditEntity;
import com.resturant.management.rms.user.UserInfm;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One cashier's session at the till: opened with a counted float, closed with
 * a counted drawer.
 *
 * <p>{@code openUserRef} is the one-open-shift rule rather than a field anyone
 * reads — it holds the cashier's id while the shift is open and null
 * afterwards, and a unique constraint does the rest. V14 explains why it is a
 * column and not the partial index the ERD asked for.
 *
 * <p>Nothing sets that key directly. {@link #open} and {@link #close} move the
 * three facts that have to agree — status, {@code closedAt} and the key —
 * together, because a setter for each would let them disagree, and the database
 * would then refuse the save with a constraint name instead of a reason.
 */
@Entity
@Table(name = "cash_shift")
@Getter
@NoArgsConstructor
public class CashShift extends BaseAuditEntity {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_ref", nullable = false)
    private UserInfm user;

    @Column(name = "open_user_ref")
    private Long openUserRef;

    /**
     * The branch half of the one-open-shift key, null once the shift closes.
     *
     * <p>V14's key was the cashier alone, which with branches would stop
     * somebody covering two shops from opening the second till. V17 widens it
     * to (branch, cashier) — the pair ERD §3.2's partial index names.
     */
    @Column(name = "open_branch_id")
    private Long openBranchId;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "opening_float", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingFloat;

    /** What the cashier counted. Null until the shift closes. */
    @Column(name = "declared_cash", precision = 12, scale = 2)
    private BigDecimal declaredCash;

    /**
     * What should have been there: the float plus every movement.
     *
     * <p>Worked out at close and stored then, because from that moment it is a
     * finding rather than a calculation — a later correction to a movement
     * must not quietly change what the drawer was measured against. Mid-shift
     * it is derived on demand and never written.
     */
    @Column(name = "expected_cash", precision = 12, scale = 2)
    private BigDecimal expectedCash;

    /** Declared minus expected. Negative is short, positive is over. */
    @Column(name = "variance", precision = 12, scale = 2)
    private BigDecimal variance;

    @Setter
    @Column(name = "note", length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ShiftStatus status = ShiftStatus.OPEN;

    /** Starts a session. The float is what was counted into the drawer. */
    public static CashShift open(UserInfm user, Long branchId, BigDecimal openingFloat) {
        CashShift shift = new CashShift();
        shift.user = user;
        shift.branchId = branchId;
        shift.openUserRef = user.getId();
        shift.openBranchId = branchId;
        shift.openedAt = LocalDateTime.now();
        shift.openingFloat = openingFloat;
        shift.status = ShiftStatus.OPEN;
        return shift;
    }

    /**
     * Ends it. The variance is recorded whatever it is — a till that refuses
     * to close on a discrepancy is a till whose cashier learns to declare the
     * expected figure.
     */
    public void close(BigDecimal declared, BigDecimal expected, String note) {
        this.declaredCash = declared;
        this.expectedCash = expected;
        this.variance = declared.subtract(expected);
        this.note = note;
        this.closedAt = LocalDateTime.now();
        this.status = ShiftStatus.CLOSED;
        // Releases the cashier to open another. The unique constraint is on
        // these two columns, so forgetting either would lock them out
        // permanently.
        this.openUserRef = null;
        this.openBranchId = null;
    }

    public boolean isOpen() {
        return status == ShiftStatus.OPEN;
    }
}
