package com.resturant.management.rms.branch;

import com.resturant.management.rms.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

/** The business. One of these owns every branch, and nothing else points at it. */
@Entity
@Table(name = "company")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Company extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "name_en", length = 150)
    private String nameEn;

    @Column(name = "tax_id", length = 50)
    private String taxId;
}
