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

    /**
     * Revenue split by category — {@code [name, nameEn, qty, revenue]}.
     *
     * <p>Joins through {@code product}, so lines whose product was later deleted
     * are excluded. The category chart can therefore total slightly less than
     * headline revenue; the report labels it as a breakdown, not a reconciliation.
     */
    @Query("""
           SELECT c.name, c.nameEn, SUM(i.qty), SUM(i.lineTotal)
           FROM OrderItem i JOIN i.product p JOIN p.category c
           WHERE i.order.status = 'PAID' AND i.order.paidAt BETWEEN :from AND :to
           GROUP BY c.id, c.name, c.nameEn
           ORDER BY SUM(i.lineTotal) DESC
           """)
    List<Object[]> revenueByCategory(@Param("from") LocalDateTime from,
                                     @Param("to") LocalDateTime to);

    /** Cost of goods sold across the range, from each product's cost price. */
    @Query("""
           SELECT COALESCE(SUM(i.qty * p.cost), 0)
           FROM OrderItem i JOIN i.product p
           WHERE i.order.status = 'PAID' AND i.order.paidAt BETWEEN :from AND :to
           """)
    java.math.BigDecimal sumCostBetween(@Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    /** Per-order cost — {@code [orderId, cost]} — for the detail table. */
    @Query("""
           SELECT i.order.id, COALESCE(SUM(i.qty * p.cost), 0)
           FROM OrderItem i JOIN i.product p
           WHERE i.order.id IN :orderIds
           GROUP BY i.order.id
           """)
    List<Object[]> costByOrder(@Param("orderIds") List<Long> orderIds);
}
