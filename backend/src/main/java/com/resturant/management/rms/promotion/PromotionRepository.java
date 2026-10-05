package com.resturant.management.rms.promotion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Page<Promotion> findAllByOrderByStartsAtDesc(Pageable pageable);

    /**
     * Rules that are switched on and inside their date range right now.
     *
     * <p>The time-of-day window is left to the service: a happy hour that
     * wraps midnight is two ranges, and expressing that in JPQL would be
     * harder to read than the two lines it takes in Java. Branch scoping is
     * automatic — Promotion carries the tenant filter.
     */
    @Query("""
           SELECT p FROM Promotion p
           WHERE p.actYn = 'Y'
             AND p.startsAt <= :now
             AND p.endsAt >= :now
           ORDER BY p.id ASC
           """)
    List<Promotion> liveAt(@Param("now") LocalDateTime now);
}
