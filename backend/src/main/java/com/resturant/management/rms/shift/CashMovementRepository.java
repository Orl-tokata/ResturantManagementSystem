package com.resturant.management.rms.shift;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {

    List<CashMovement> findByShiftIdOrderByIdAsc(Long shiftId);

    /** {@code [type, total, count]} for the Z-report's breakdown. */
    @Query("""
           SELECT m.type, COALESCE(SUM(m.amount), 0), COUNT(m)
           FROM CashMovement m
           WHERE m.shift.id = :shiftId
           GROUP BY m.type
           """)
    List<Object[]> totalsByType(@Param("shiftId") Long shiftId);
}
