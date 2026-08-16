package com.resturant.management.rms.catalog;

import com.resturant.management.rms.common.BaseAuditEntity;
import com.resturant.management.rms.common.RecordStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Khmer name, e.g. បាយ. */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** English name, e.g. Rice. */
    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "icon", length = 20)
    private String icon;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;
}
