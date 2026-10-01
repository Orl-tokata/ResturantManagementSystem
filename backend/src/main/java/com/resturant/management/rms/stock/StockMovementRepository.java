package com.resturant.management.rms.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByStockItemIdOrderByCreatedAtDesc(Long stockItemId, Pageable pageable);

    Page<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    /** The whole ledger, newest first — both products and ingredients. */
    Page<StockMovement> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    long countByStockItemId(Long stockItemId);

    List<StockMovement> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}
