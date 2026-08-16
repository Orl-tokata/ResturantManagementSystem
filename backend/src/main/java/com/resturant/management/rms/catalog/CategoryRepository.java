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
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByStatusOrderBySortOrderAsc(RecordStatus status);

    @Query("""
           SELECT c FROM Category c
           WHERE :q IS NULL
              OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(c.nameEn) LIKE LOWER(CONCAT('%', :q, '%'))
           """)
    Page<Category> search(@Param("q") String q, Pageable pageable);
}
