package com.resturant.management.rms.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query(value = "SELECT nextval('seq_customer_code')", nativeQuery = true)
    Long nextCodeSequence();

    /**
     * The admin list's search: name, phone or code, case-insensitively.
     *
     * <p>{@code :q} is normalised to a non-null string by the service, because
     * `:param IS NULL OR ...` is accepted by H2 and rejected by PostgreSQL —
     * which is how the audit screen reached production returning 500s.
     */
    @Query("""
           SELECT c FROM Customer c
           WHERE :q = ''
              OR LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
              OR LOWER(c.code) LIKE LOWER(CONCAT('%', :q, '%'))
              OR c.phone LIKE CONCAT('%', :q, '%')
           """)
    Page<Customer> search(@Param("q") String q, Pageable pageable);

    /**
     * The till's lookup: an exact phone.
     *
     * <p>A list, because the column is not unique and pretending otherwise
     * would silently pick one of a couple who share a number. The caller
     * decides what to do with two.
     */
    List<Customer> findByPhoneOrderByIdAsc(String phone);

    Optional<Customer> findByCode(String code);
}
