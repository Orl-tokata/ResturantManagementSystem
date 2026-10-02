package com.resturant.management.rms.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SalePaymentRepository extends JpaRepository<SalePayment, Long> {

    List<SalePayment> findByOrderIdOrderByIdAsc(Long orderId);

    /** The code currently on screen for this bill, if there is one. */
    Optional<SalePayment> findFirstByOrderIdAndStatusOrderByIdDesc(Long orderId, PaymentStatus status);

    /**
     * What each method took in, over a date range.
     *
     * <p>By {@code created_at} rather than the order's {@code paid_at}: with a
     * split the two can differ, and a drawer is counted by when the money was
     * put in it.
     */
    @Query("""
           SELECT p.method, COALESCE(SUM(p.amount), 0), COUNT(p)
           FROM SalePayment p
           WHERE p.status = com.resturant.management.rms.order.PaymentStatus.CAPTURED
             AND p.createdAt BETWEEN :from AND :to
           GROUP BY p.method
           ORDER BY SUM(p.amount) DESC
           """)
    List<Object[]> capturedByMethod(@Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);
}
