package com.resturant.management.rms.dining;

import com.resturant.management.rms.audit.Audited;
import com.resturant.management.rms.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

/**
 * Named {@code DiningTable} rather than {@code Table} to avoid colliding with
 * {@link jakarta.persistence.Table}.
 */
@Audited(ignore = "status")   // flips on every order; the seating plan is what matters
@Entity
@jakarta.persistence.Table(name = "dining_table")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiningTable extends BaseAuditEntity {

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

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "seats", nullable = false)
    private Integer seats;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "zone", nullable = false, length = 20)
    private TableZone zone = TableZone.INDOOR;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TableStatus status = TableStatus.FREE;
}
