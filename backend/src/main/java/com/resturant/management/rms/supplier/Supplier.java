package com.resturant.management.rms.supplier;

import com.resturant.management.rms.common.BaseAuditEntity;
import com.resturant.management.rms.common.RecordStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "supplier")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_code", nullable = false, unique = true, length = 20)
    private String supplierCode;

    @Column(name = "company", nullable = false, length = 150)
    private String company;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 120)
    private String email;

    /** Free text for now (MEAT, VEGETABLE, SEAFOOD, DRINK, RICE …). */
    @Column(name = "supply_type", length = 30)
    private String supplyType;

    @Column(name = "address", length = 500)
    private String address;

    /** Outstanding payable to this supplier. */
    @Builder.Default
    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RecordStatus status = RecordStatus.ACTIVE;
}
