package com.resturant.management.rms.catalog;

import com.resturant.management.rms.common.BaseAuditEntity;
import com.resturant.management.rms.common.RecordStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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
    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "description", length = 1000)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;
}
