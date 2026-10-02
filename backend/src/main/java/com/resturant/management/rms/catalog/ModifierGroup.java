package com.resturant.management.rms.catalog;

import com.resturant.management.rms.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A question asked about a dish: "sugar level", "anything to add".
 *
 * <p>Shared across the menu rather than owned by one product, because the
 * question is the same whoever is asking it — attaching "sugar level" to six
 * drinks should not mean six copies to keep in step. Which dishes ask it is
 * recorded on {@link Product}.
 *
 * <p>Unscoped, like categories and suppliers (V17): one company's shops share
 * a menu structure. When that stops being true it is a migration, not a
 * surprise.
 */
@Entity
@Table(name = "modifier_group")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierGroup extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    /**
     * The shape of the question. 0..1 is "would you like anything", 1..1 is
     * "you must choose one", 0..n is "tick what you want".
     */
    @Builder.Default
    @Column(name = "min_select", nullable = false)
    private Integer minSelect = 0;

    @Builder.Default
    @Column(name = "max_select", nullable = false)
    private Integer maxSelect = 1;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @OrderBy("sortOrder ASC, id ASC")
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Modifier> modifiers = new ArrayList<>();

    public void addModifier(Modifier modifier) {
        modifiers.add(modifier);
        modifier.setGroup(this);
    }

    /** True when the customer has to answer. */
    public boolean isRequired() {
        return minSelect != null && minSelect > 0;
    }
}
