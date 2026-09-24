package com.resturant.management.rms.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    long countByCategoryId(Long categoryId);

    /** Backs the POS grid: optional text search plus optional category filter. */
    @Query("""
           SELECT p FROM Product p
           WHERE (:q IS NULL
                  OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%'))
                  OR LOWER(p.nameEn) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')))
             AND (:categoryId IS NULL OR p.category.id = :categoryId)
           """)
    Page<Product> search(@Param("q") String q,
                         @Param("categoryId") Long categoryId,
                         Pageable pageable);

    /** Dashboard "low stock" tile. */
    @Query("SELECT p FROM Product p WHERE p.stockQty <= :threshold ORDER BY p.stockQty ASC")
    List<Product> findLowStock(@Param("threshold") java.math.BigDecimal threshold);
}
