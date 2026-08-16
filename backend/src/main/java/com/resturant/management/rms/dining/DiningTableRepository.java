package com.resturant.management.rms.dining;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    Optional<DiningTable> findByName(String name);

    boolean existsByName(String name);

    List<DiningTable> findByStatus(TableStatus status);

    long countByStatus(TableStatus status);

    @Query("""
           SELECT t FROM DiningTable t
           WHERE (:q IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%')))
             AND (:zone IS NULL OR t.zone = :zone)
           ORDER BY t.name ASC
           """)
    Page<DiningTable> search(@Param("q") String q,
                             @Param("zone") TableZone zone,
                             Pageable pageable);
}
