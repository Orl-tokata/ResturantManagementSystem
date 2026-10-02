package com.resturant.management.rms.branch;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    List<Branch> findByActYnOrderByCodeAsc(String actYn);

    Optional<Branch> findByCode(String code);
}
