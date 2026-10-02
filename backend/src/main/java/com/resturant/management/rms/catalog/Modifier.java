package com.resturant.management.rms.catalog;

import com.resturant.management.rms.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** One possible answer: "no ice", "extra shot", "takeaway box". */
@Entity
@Table(name = "modifier")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Modifier extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ModifierGroup group;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    /**
     * What choosing it does to the line's price. Signed: "no ice" is free,
     * "extra shot" costs, and leaving something out can legitimately take
     * money off.
     */
    @Builder.Default
    @Column(name = "price_delta", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
