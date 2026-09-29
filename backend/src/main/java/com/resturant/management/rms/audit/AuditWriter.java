package com.resturant.management.rms.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Writes finished audit entries in a transaction of their own.
 *
 * <p>A separate bean rather than a method on {@link AuditService} because
 * {@code @Transactional} is applied by a proxy, and a call from one method of a
 * class to another goes straight past it. The annotation would have been
 * decoration, and the failure would have been silent: entries written with no
 * transaction, inside an {@code afterCommit} callback where there is none.
 */
@Component
@RequiredArgsConstructor
public class AuditWriter {

    private final AuditLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(List<AuditLog> entries) {
        repository.saveAll(entries);
    }
}
