package com.resturant.management.rms.customer;

import com.resturant.management.rms.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Someone who eats here more than once.
 *
 * <p>No points balance. The balance is the sum of {@link LoyaltyTransaction},
 * and a column holding it as well would be a second answer to one question with
 * nobody able to say which is right — the same reasoning that keeps
 * {@code expected_cash} out of an open shift.
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Generated, so a counter staff member never has to invent one. */
    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * The lookup key at the till, and deliberately not unique: a number is
     * shared between a couple, reassigned by the carrier and mistyped, and a
     * unique constraint would turn any of those into a blocked sale.
     */
    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "note", length = 500)
    private String note;
}
