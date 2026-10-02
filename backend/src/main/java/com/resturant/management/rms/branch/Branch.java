package com.resturant.management.rms.branch;

import com.resturant.management.rms.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * One shop.
 *
 * <p>Every scoped row carries this id, and the id comes from the signed token
 * rather than from the request — API §3. Deliberately not tenant-scoped
 * itself: a user has to be able to see the branches they may switch to, which
 * is a question about branches asked from inside one of them.
 */
@Entity
@Table(name = "branch")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Branch extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** Short, unique within the company, and what a report groups by. */
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "name_en", length = 150)
    private String nameEn;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone", length = 30)
    private String phone;
}
