package com.resturant.management.rms.audit;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Read and append only. There is deliberately no update path — the inherited
 * {@code delete*} methods are never called, and nothing exposes them.
 *
 * <p>Filtering is built as a {@link Specification} rather than written as
 * {@code :param IS NULL OR column = :param} in a JPQL string. That idiom works
 * on H2 and fails on PostgreSQL, which cannot infer the type of a bare
 * parameter compared only against NULL — {@code could not determine data type
 * of parameter $7}. A specification emits only the predicates actually asked
 * for, so the question never arises, and the SQL is smaller for it.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>,
        JpaSpecificationExecutor<AuditLog> {

    static Specification<AuditLog> matching(String entity, Long entityId, String userId,
                                            LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> where = new ArrayList<>();
            if (entity != null)   where.add(cb.equal(root.get("entity"), entity));
            if (entityId != null) where.add(cb.equal(root.get("entityId"), entityId));
            if (userId != null)   where.add(cb.equal(root.get("userId"), userId));
            if (from != null)     where.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null)       where.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return where.isEmpty() ? cb.conjunction() : cb.and(where.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
