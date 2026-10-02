package com.resturant.management.rms.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface LoyaltyRepository extends JpaRepository<LoyaltyTransaction, Long> {

    Page<LoyaltyTransaction> findByCustomerIdOrderByIdDesc(Long customerId, Pageable pageable);

    /** The balance: the sum of the ledger, never a stored column. */
    @Query("""
           SELECT COALESCE(SUM(t.points), 0)
           FROM LoyaltyTransaction t
           WHERE t.customer.id = :customerId
           """)
    BigDecimal balanceOf(@Param("customerId") Long customerId);

    /**
     * Balances for a page of customers in one query — {@code [customerId, points]}.
     *
     * <p>The list screen shows a balance per row, and asking per row is the
     * N+1 that makes a list of twenty customers twenty-one queries.
     */
    @Query("""
           SELECT t.customer.id, COALESCE(SUM(t.points), 0)
           FROM LoyaltyTransaction t
           WHERE t.customer.id IN :ids
           GROUP BY t.customer.id
           """)
    List<Object[]> balancesOf(@Param("ids") List<Long> ids);

    boolean existsByOrderIdAndType(Long orderId, LoyaltyType type);

    /** What this bill gave out, for a return to take a share back. */
    @Query("""
           SELECT COALESCE(SUM(t.points), 0)
           FROM LoyaltyTransaction t
           WHERE t.order.id = :orderId
             AND t.type = com.resturant.management.rms.customer.LoyaltyType.EARN
           """)
    BigDecimal earnedFor(@Param("orderId") Long orderId);
}
