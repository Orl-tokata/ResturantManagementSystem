package com.resturant.management.rms.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserInfm, Long> {

    Optional<UserInfm> findByUserId(String userId);

    Optional<UserInfm> findByEml(String eml);

    boolean existsByUserId(String userId);

    boolean existsByEml(String eml);

    Page<UserInfm> findByRole(Role role, Pageable pageable);
}
