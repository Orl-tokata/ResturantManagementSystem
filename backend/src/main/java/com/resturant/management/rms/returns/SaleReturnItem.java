package com.resturant.management.rms.returns;

import com.resturant.management.rms.order.OrderItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One line given back.
 *
 * <p>It points at the line of the original sale rather than at a product, which
 * is what makes "how many of this line are still returnable" answerable at all
 * — and what prices the refund at what was actually charged, not at what the
 * dish costs today.
 */
@Entity
@Table(name = "sale_return_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    private SaleReturn saleReturn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "qty", nullable = false, precision = 12, scale = 2)
    private BigDecimal qty;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;
}
