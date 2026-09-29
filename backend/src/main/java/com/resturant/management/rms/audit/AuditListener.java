package com.resturant.management.rms.audit;

import lombok.RequiredArgsConstructor;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.temporal.Temporal;
import java.util.*;

/**
 * Turns Hibernate's own change events into audit entries.
 *
 * <p>A listener rather than an aspect over service methods, because an aspect
 * only records what someone remembered to annotate — and it cannot answer "from
 * what, to what" without re-reading the row before the change. Hibernate
 * already holds both states at flush time, which is exactly the question
 * docs/ERD.md §3.8 asks the log to answer.
 *
 * <p>Only entities marked {@link Audited} are recorded. Everything is captured
 * here and now, not when the row is finally written: by then the transaction
 * has committed and Spring Security has cleared the context, so the user would
 * be anonymous on every entry.
 */
@Component
@RequiredArgsConstructor
public class AuditListener
        implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Attributed to changes with no signed-in user: startup seeding, mostly. */
    private static final String SYSTEM = "system";

    /**
     * The audit stamps every business entity carries, skipped for all of them.
     *
     * <p>They change on every update by definition, so including them would
     * put two fields of bookkeeping beside each real change — and an update
     * that touched nothing else would still write a row saying, in effect,
     * "something was saved". The log already records who and when, better.
     *
     * <p>{@code actYn} is deliberately not here: a row being deactivated is a
     * real change and one of the more interesting ones.
     */
    private static final Set<String> ALWAYS_IGNORED =
            Set.of("regId", "regDtm", "modId", "modDtm");

    private final transient AuditService auditService;

    /* ---- Events ----------------------------------------------------------- */

    @Override
    public void onPostInsert(PostInsertEvent event) {
        Audited audited = auditedOn(event.getEntity());
        if (audited == null) return;

        Map<String, String> after = readAll(
                event.getPersister().getPropertyNames(), event.getState(), audited);
        record(audited, event.getEntity(), event.getId(), AuditLog.Action.CREATE, null, after);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        Audited audited = auditedOn(event.getEntity());
        if (audited == null) return;

        Map<String, String> before = new LinkedHashMap<>();
        Map<String, String> after = new LinkedHashMap<>();
        String[] names = event.getPersister().getPropertyNames();
        Object[] oldState = event.getOldState();
        Object[] newState = event.getState();

        // Null when the entity was not loaded in this session — a detached
        // merge, for instance. Nothing to compare against, so the change is
        // recorded without a before rather than dropped.
        if (oldState == null) {
            record(audited, event.getEntity(), event.getId(), AuditLog.Action.UPDATE,
                    null, readAll(names, newState, audited));
            return;
        }

        int[] dirty = event.getDirtyProperties();
        int[] indices = dirty != null ? dirty : allIndices(names.length);

        for (int i : indices) {
            if (i >= names.length) continue;
            String name = names[i];
            if (skip(name, audited)) continue;
            String from = render(oldState[i]);
            String to = render(newState[i]);
            if (Objects.equals(from, to)) continue;
            before.put(name, from);
            after.put(name, to);
        }

        // A dirty flag that turns out to change nothing — a value reassigned to
        // itself, or a field this listener does not record. Not worth a row.
        if (after.isEmpty()) return;

        record(audited, event.getEntity(), event.getId(), AuditLog.Action.UPDATE, before, after);
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        Audited audited = auditedOn(event.getEntity());
        if (audited == null) return;

        Map<String, String> before = readAll(
                event.getPersister().getPropertyNames(), event.getDeletedState(), audited);
        record(audited, event.getEntity(), event.getId(), AuditLog.Action.DELETE, before, null);
    }

    /** Hibernate asks whether it must load the row before updating; it need not for this. */
    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    /* ---- Building the entry ------------------------------------------------ */

    private void record(Audited audited, Object entity, Object id, AuditLog.Action action,
                        Map<String, String> before, Map<String, String> after) {

        String name = audited.value().isBlank() ? entity.getClass().getSimpleName() : audited.value();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())
                ? SYSTEM
                : auth.getName();

        auditService.capture(AuditLog.builder()
                .userId(user)
                .action(action)
                .entity(name)
                .entityId(id instanceof Number n ? n.longValue() : null)
                .beforeJson(auditService.toJson(before))
                .afterJson(auditService.toJson(after))
                .ip(callerIp())
                .createdAt(java.time.LocalDateTime.now())
                .build());
    }

    private Audited auditedOn(Object entity) {
        return entity == null ? null : entity.getClass().getAnnotation(Audited.class);
    }

    private Map<String, String> readAll(String[] names, Object[] state, Audited audited) {
        if (names == null || state == null) return Map.of();
        Map<String, String> out = new LinkedHashMap<>();
        for (int i = 0; i < names.length && i < state.length; i++) {
            if (skip(names[i], audited)) continue;
            String value = render(state[i]);
            if (value != null) out.put(names[i], value);
        }
        return out;
    }

    private boolean skip(String field, Audited audited) {
        if (ALWAYS_IGNORED.contains(field)) return true;
        for (String redacted : audited.redact()) {
            if (redacted.equals(field)) return true;
        }
        for (String ignored : audited.ignore()) {
            if (ignored.equals(field)) return true;
        }
        return false;
    }

    /**
     * A value worth putting in a log, or nothing.
     *
     * <p>Associations and collections are skipped rather than stringified: a
     * lazy proxy renders as a class name and a hash, which is noise, and
     * touching one here would trigger a load during flush.
     */
    private String render(Object value) {
        if (value == null) return null;
        if (value instanceof String s) return s;
        if (value instanceof BigDecimal d) {
            // Scale is not a change. The old value comes from the database at
            // the column's scale and the new one from the request as it was
            // typed, so 2.00 and 2 would otherwise be logged as an edit — a
            // false entry in the one table that is supposed to be trustworthy.
            return d.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Number || value instanceof Boolean
                || value instanceof Enum<?> || value instanceof Temporal
                || value instanceof java.util.Date || value instanceof Character) {
            return String.valueOf(value);
        }
        return null;
    }

    private static int[] allIndices(int length) {
        int[] all = new int[length];
        for (int i = 0; i < length; i++) all[i] = i;
        return all;
    }

    /** Null outside a web request — a scheduled job or startup seeding. */
    private String callerIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        String ip = attrs.getRequest().getRemoteAddr();
        return ip == null || ip.length() > 45 ? null : ip;
    }
}
