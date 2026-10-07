package com.resturant.management.rms.catalog;

import com.resturant.management.rms.common.RecordStatus;
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

    /**
     * Backs both the catalogue screen and the POS grid.
     *
     * <p>{@code active} is what tells them apart. The admin list passes null
     * and sees everything, because an inactive dish still has to be found and
     * edited. The till passes ACTIVE and requires it of the category too:
     * switching a category off left its dishes on sale with no button to
     * reach them by, so they could be sold but not browsed to.
     */
    @Query("""
           SELECT p FROM Product p
           WHERE (:q IS NULL
                  OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%'))
                  OR LOWER(p.nameEn) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')))
             AND (:categoryId IS NULL OR p.category.id = :categoryId)
             AND (:active IS NULL
                  OR (p.status = :active AND p.category.status = :active))
           """)
    Page<Product> search(@Param("q") String q,
                         @Param("active") RecordStatus active,
                         @Param("categoryId") Long categoryId,
                         Pageable pageable);

    /** Dashboard "low stock" tile. */
    @Query("SELECT p FROM Product p WHERE p.stockQty <= :threshold ORDER BY p.stockQty ASC")
    List<Product> findLowStock(@Param("threshold") java.math.BigDecimal threshold);
}
