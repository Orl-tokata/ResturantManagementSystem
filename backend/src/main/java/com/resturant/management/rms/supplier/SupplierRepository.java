package com.resturant.management.rms.supplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findBySupplierCode(String supplierCode);

    boolean existsBySupplierCode(String supplierCode);

    @Query("""
           SELECT s FROM Supplier s
           WHERE :q IS NULL
              OR LOWER(s.company) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%'))
              OR LOWER(s.supplierCode) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%'))
              OR LOWER(s.contactPerson) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%'))
           """)
    Page<Supplier> search(@Param("q") String q, Pageable pageable);

    java.util.List<Supplier> findByStatus(com.resturant.management.rms.common.RecordStatus status);

    /** Total outstanding to all suppliers — the payable tile. */
    @Query("SELECT COALESCE(SUM(s.balance), 0) FROM Supplier s")
    java.math.BigDecimal sumBalances();
}
