package com.resturant.management.rms.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByInvoiceNo(String invoiceNo);

    List<Order> findByTableIdAndStatus(Long tableId, OrderStatus status);

    /** Next invoice number, handed out by the DB sequence — see V1__baseline.sql. */
    @Query(value = "SELECT nextval('seq_invoice_no')", nativeQuery = true)
    Long nextInvoiceSequence();

    @Query("""
           SELECT o FROM Order o
           WHERE (:status IS NULL OR o.status = :status)
             AND (:from IS NULL OR o.regDtm >= :from)
             AND (:to   IS NULL OR o.regDtm <= :to)
             AND (:q IS NULL OR LOWER(o.invoiceNo) LIKE LOWER(CONCAT('%', :q, '%')))
           ORDER BY o.regDtm DESC
           """)
    Page<Order> search(@Param("q") String q,
                       @Param("status") OrderStatus status,
                       @Param("from") LocalDateTime from,
                       @Param("to") LocalDateTime to,
                       Pageable pageable);

    @Query("""
           SELECT COALESCE(SUM(o.total), 0) FROM Order o
           WHERE o.status = 'PAID' AND o.paidAt BETWEEN :from AND :to
           """)
    BigDecimal sumRevenueBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByStatusAndPaidAtBetween(OrderStatus status, LocalDateTime from, LocalDateTime to);

    /* ---- History screen -------------------------------------------------
       Everything here filters on regDtm — when the bill was opened — so the
       tiles always describe exactly the rows the table below them is showing.
       Revenue reporting in milestone 12 uses paidAt instead, which is the
       right basis for money actually taken.
       -------------------------------------------------------------------- */

    long countByRegDtmBetween(LocalDateTime from, LocalDateTime to);

    long countByStatusAndRegDtmBetween(OrderStatus status, LocalDateTime from, LocalDateTime to);

    @Query("""
           SELECT COALESCE(SUM(o.total), 0) FROM Order o
           WHERE o.status = 'PAID' AND o.regDtm BETWEEN :from AND :to
           """)
    BigDecimal sumPaidTotalByRegDtmBetween(@Param("from") LocalDateTime from,
                                           @Param("to") LocalDateTime to);
}
