package com.resturant.management.rms.staff;

import com.resturant.management.rms.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByStaffCode(String staffCode);

    boolean existsByStaffCode(String staffCode);

    Page<Staff> findByRole(Role role, Pageable pageable);

    @Query("""
           SELECT s FROM Staff s
           WHERE (:q IS NULL
                  OR LOWER(s.staffName) LIKE LOWER(CONCAT('%', :q, '%'))
                  OR LOWER(s.staffCode) LIKE LOWER(CONCAT('%', :q, '%'))
                  OR s.phone LIKE CONCAT('%', :q, '%'))
             AND (:role IS NULL OR s.role = :role)
           """)
    Page<Staff> search(@Param("q") String q, @Param("role") Role role, Pageable pageable);
}
