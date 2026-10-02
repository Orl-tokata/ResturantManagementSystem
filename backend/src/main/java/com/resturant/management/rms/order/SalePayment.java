package com.resturant.management.rms.order;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One payment against one bill.
 *
 * <p>A bill has a list of these rather than three columns, so it can be settled
 * by more than one method, so a code that was shown and never paid leaves a
 * record, and so a refund has something to point at.
 *
 * <p>Nothing here is derived from the order. {@code amount} is what this
 * payment covered, not the bill's total; for a single tender they are the same
 * number and for a split they are not, which is the whole point.
 */
@Entity
@Table(name = "sale_payment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private PaymentMethod method;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** The same money in riel, at the rate the bill was stamped with. */
    @Column(name = "amount_khr", precision = 14, scale = 0)
    private BigDecimal amountKhr;

    /**
     * What the customer handed over, and what they got back. Cash only — on
     * every other method these stay null rather than repeating {@code amount}
     * and reading as though notes had been counted out.
     */
    @Column(name = "tendered", precision = 12, scale = 2)
    private BigDecimal tendered;

    @Column(name = "change_amount", precision = 12, scale = 2)
    private BigDecimal changeAmount;

    /** The bank's reference, or whatever the cashier was given for a transfer. */
    @Column(name = "reference", length = 100)
    private String reference;

    /** How Bakong identifies this transaction — see V12 on the two md5s. */
    @Column(name = "khqr_md5", length = 32)
    private String khqrMd5;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.CAPTURED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Only captured money settles a bill. */
    public boolean isCaptured() {
        return status == PaymentStatus.CAPTURED;
    }
}
