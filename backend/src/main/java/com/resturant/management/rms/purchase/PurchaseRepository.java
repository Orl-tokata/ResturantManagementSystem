package com.resturant.management.rms.purchase;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    Optional<Purchase> findByPoNo(String poNo);

    /** Next PO number, handed out by the DB sequence — see V1__baseline.sql. */
    @Query(value = "SELECT nextval('seq_purchase_no')", nativeQuery = true)
    Long nextPurchaseSequence();

    @Query("""
           SELECT p FROM Purchase p
           WHERE (:q IS NULL
                  OR LOWER(p.poNo) LIKE LOWER(CONCAT('%', :q, '%'))
                  OR LOWER(p.supplier.company) LIKE LOWER(CONCAT('%', :q, '%')))
             AND (:status IS NULL OR p.status = :status)
           ORDER BY p.purchaseDate DESC
           """)
    Page<Purchase> search(@Param("q") String q,
                          @Param("status") PurchaseStatus status,
                          Pageable pageable);

    @Query("""
           SELECT COALESCE(SUM(p.total), 0) FROM Purchase p
           WHERE p.status <> 'CANCELLED' AND p.purchaseDate BETWEEN :from AND :to
           """)
    BigDecimal sumTotalBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
