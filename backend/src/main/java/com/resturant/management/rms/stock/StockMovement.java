package com.resturant.management.rms.stock;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Append-only ledger of stock changes. Every adjustment, receipt and sale
 * deduction writes a row here, so {@code StockItem.qty} can always be audited
 * against its history.
 */
@Entity
@Table(name = "stock_movement")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_item_id", nullable = false)
    private StockItem stockItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    /** Always positive — direction is carried by {@link #movementType}. */
    @Column(name = "qty", nullable = false, precision = 12, scale = 2)
    private BigDecimal qty;

    /**
     * The product this moved, when it is a product rather than an ingredient.
     *
     * <p>Exactly one of this and {@link #stockItem} is set; the database
     * enforces it rather than trusting the code to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private com.resturant.management.rms.catalog.Product product;

    /** What caused this, e.g. {@code ORDER}. Null for a manual adjustment. */
    @Column(name = "ref_type", length = 30)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    /**
     * The balance immediately after this movement.
     *
     * <p>Null on rows written before V11: the balance before them was never
     * recorded, so any figure would be a guess presented as a record.
     */
    @Column(name = "balance_after", precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
