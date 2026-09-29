package com.resturant.management.rms.audit;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.context.annotation.Configuration;

/**
 * Hooks {@link AuditListener} into Hibernate once the factory exists.
 *
 * <p>Registering from {@code @PostConstruct} rather than through an
 * {@code Integrator}: an Integrator is built by Hibernate before the Spring
 * context is ready, so it cannot be handed a Spring bean, and the listener
 * needs one to write anything.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AuditListenerRegistrar {

    private final EntityManagerFactory entityManagerFactory;
    private final AuditListener auditListener;

    @PostConstruct
    public void register() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry()
                .getService(EventListenerRegistry.class);

        if (registry == null) {
            // Better to say so than to leave an audit log that is quietly empty.
            log.error("Hibernate EventListenerRegistry unavailable; changes will NOT be audited");
            return;
        }

        // The flush-time events, not the POST_COMMIT_* ones. These fire while
        // the transaction is still open, which is what lets AuditService
        // register a synchronization and write after it commits. A POST_COMMIT
        // listener runs when there is no transaction left to hang that on.
        registry.appendListeners(EventType.POST_INSERT, auditListener);
        registry.appendListeners(EventType.POST_UPDATE, auditListener);
        registry.appendListeners(EventType.POST_DELETE, auditListener);
        log.info("Audit listener registered for insert, update and delete");
    }
}
