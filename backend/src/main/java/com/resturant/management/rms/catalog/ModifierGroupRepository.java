package com.resturant.management.rms.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, Long> {

    List<ModifierGroup> findByActYnOrderBySortOrderAscIdAsc(String actYn);
}
