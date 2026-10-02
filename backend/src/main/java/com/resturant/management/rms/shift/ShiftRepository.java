package com.resturant.management.rms.shift;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ShiftRepository extends JpaRepository<CashShift, Long> {

    /**
     * The shift this cashier has open, if any.
     *
     * <p>By {@code openUserRef} rather than by user and status, because that
     * column is the one the unique constraint is on: asking the question the
     * same way the database answers it means the two cannot disagree.
     */
    Optional<CashShift> findByOpenUserRef(Long userId);

    Page<CashShift> findAllByOrderByOpenedAtDesc(Pageable pageable);

    Page<CashShift> findByUserIdOrderByOpenedAtDesc(Long userId, Pageable pageable);

    /**
     * The drawer's movements, added up with their signs.
     *
     * <p>Derived on every read and never stored while the shift is open: a
     * running total in a column is a number that can drift from the rows it
     * claims to summarise.
     */
    @Query("""
           SELECT COALESCE(SUM(CASE WHEN m.type IN (com.resturant.management.rms.shift.CashMovementType.SALE,
                                                    com.resturant.management.rms.shift.CashMovementType.PAY_IN,
                                                    com.resturant.management.rms.shift.CashMovementType.FLOAT)
                                    THEN m.amount ELSE -m.amount END), 0)
           FROM CashMovement m
           WHERE m.shift.id = :shiftId
           """)
    BigDecimal netMovement(@Param("shiftId") Long shiftId);
}
