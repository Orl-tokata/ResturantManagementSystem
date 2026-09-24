package com.resturant.management.rms.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockItemRepository extends JpaRepository<StockItem, Long> {

    @Query("SELECT s FROM StockItem s WHERE s.qty < s.minQty ORDER BY s.qty ASC")
    List<StockItem> findLowStock();

    @Query("SELECT COUNT(s) FROM StockItem s WHERE s.qty < s.minQty")
    long countLowStock();

    @Query("SELECT COUNT(s) FROM StockItem s WHERE s.qty <= 0")
    long countOutOfStock();

    @Query("SELECT COALESCE(SUM(s.qty * s.unitCost), 0) FROM StockItem s")
    java.math.BigDecimal totalStockValue();

    @Query("""
           SELECT s FROM StockItem s
           WHERE :q IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%'))
           """)
    Page<StockItem> search(@Param("q") String q, Pageable pageable);
}
