package com.resturant.management.rms.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

    /**
     * Best sellers for the dashboard.
     * Each row is {@code [productName, totalQty, totalRevenue]}.
     */
    @Query("""
           SELECT i.productName, SUM(i.qty), SUM(i.lineTotal)
           FROM OrderItem i
           WHERE i.order.status = 'PAID' AND i.order.paidAt BETWEEN :from AND :to
           GROUP BY i.productName
           ORDER BY SUM(i.qty) DESC
           """)
    List<Object[]> findBestSellers(@Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to);
}
