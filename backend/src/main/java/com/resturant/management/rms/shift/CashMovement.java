package com.resturant.management.rms.shift;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One thing that happened to the cash in a drawer. */
@Entity
@Table(name = "cash_movement")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashMovement {

    /** What caused it, for {@code refType}. */
    public static final String REF_ORDER = "ORDER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private CashShift shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CashMovementType type;

    /** Always positive; {@link CashMovementType} carries the direction. */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "ref_type", length = 30)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** What this did to the drawer: positive in, negative out. */
    public BigDecimal signedAmount() {
        return type.applyTo(amount);
    }
}
