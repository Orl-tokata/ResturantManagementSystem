package com.resturant.management.rms.branch;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Hands Hibernate the branch, on every session.
 *
 * <p>Entities annotated {@code @TenantId} get the value written on insert and
 * the column added to the WHERE clause of every query Hibernate builds —
 * including {@code findById}. That is the point: scoping that no repository
 * method can forget, because no repository method is involved.
 *
 * <p>What it does not cover is native SQL. The only native queries here are
 * four {@code nextval} calls for document numbers, which have no rows to
 * scope; anything native added later has to carry its own branch, and this
 * comment is where that will be read from.
 */
@Component
public class BranchTenantResolver
        implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        return BranchContext.get();
    }

    /**
     * False, deliberately.
     *
     * <p>Returning true asks Hibernate to check that a session kept across a
     * tenant change is not reused. Sessions here are per-transaction and the
     * branch cannot change inside one, and the check costs a comparison on
     * every operation for a situation this application cannot produce.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> properties) {
        properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
