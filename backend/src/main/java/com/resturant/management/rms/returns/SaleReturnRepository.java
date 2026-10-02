package com.resturant.management.rms.returns;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, Long> {

    @Query(value = "SELECT nextval('seq_return_no')", nativeQuery = true)
    Long nextReturnSequence();

    Page<SaleReturn> findAllByOrderByIdDesc(Pageable pageable);

    Page<SaleReturn> findByOrderIdOrderByIdDesc(Long orderId, Pageable pageable);

    List<SaleReturn> findByOrderId(Long orderId);

    /**
     * How much of each line has already gone back — {@code [orderItemId, qty]}.
     *
     * <p>The sum no CHECK can express (ERD §3.7): returning three of a line of
     * two is only visible by adding up every return document for that line. One
     * query for the whole order, because the alternative is one per line and
     * the answer has to be consistent across all of them anyway.
     */
    @Query("""
           SELECT i.orderItem.id, COALESCE(SUM(i.qty), 0)
           FROM SaleReturnItem i
           WHERE i.saleReturn.order.id = :orderId
           GROUP BY i.orderItem.id
           """)
    List<Object[]> returnedByLine(@Param("orderId") Long orderId);

    @Query("""
           SELECT COALESCE(SUM(r.total), 0)
           FROM SaleReturn r
           WHERE r.regDtm BETWEEN :from AND :to
           """)
    BigDecimal totalBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
