package com.resturant.management.rms.dining;

import com.resturant.management.rms.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Named {@code DiningTable} rather than {@code Table} to avoid colliding with
 * {@link jakarta.persistence.Table}.
 */
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
