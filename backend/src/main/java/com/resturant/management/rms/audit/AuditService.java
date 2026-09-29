package com.resturant.management.rms.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Collects changes during a transaction and writes them once it has committed.
 *
 * <p>They cannot be written as they happen: the rows are produced from inside
 * Hibernate's flush, and inserting into the same session mid-flush is how you
 * get a {@code ConcurrentModificationException} instead of an audit trail.
 *
 * <p>Writing after the commit means a change is recorded only if it really
 * happened, which is the right way round — a log of things that were rolled
 * back would be worse than none. The cost is the narrow window where the
 * business data commits and the audit write then fails; that is logged loudly
 * and deliberately does not fail the request. A till that stops selling because
 * its audit log is full is a worse outcome than a missing row, and the row is
 * recoverable from the application log.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    /** Matches {@code before_json}/{@code after_json} in V7. */
    static final int MAX_JSON = 4000;

    private final AuditWriter writer;
    private final ObjectMapper objectMapper;

    private static final ThreadLocal<List<AuditLog>> PENDING = new ThreadLocal<>();

    /**
     * Queues one change for the end of the current transaction.
     *
     * <p>Outside a transaction the entry is dropped rather than written
     * immediately — nothing in this application changes audited data outside
     * one, so an entry arriving here without a transaction means something
     * unexpected happened and inventing a commit boundary for it would hide
     * that.
     */
    public void capture(AuditLog entry) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("Audited change to {} {} outside a transaction; not recorded",
                    entry.getEntity(), entry.getEntityId());
            return;
        }

        List<AuditLog> pending = PENDING.get();
        if (pending == null) {
            pending = new ArrayList<>();
            PENDING.set(pending);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    List<AuditLog> entries = PENDING.get();
                    if (entries != null && !entries.isEmpty()) {
                        // Qualified: TransactionSynchronization declares its own
                        // no-arg flush(), which an unqualified call resolves to.
                        writeEntries(List.copyOf(entries));
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    // Also runs after a rollback, where afterCommit does not.
                    // Without this the entries would leak into whatever the
                    // thread handles next and be attributed to it.
                    PENDING.remove();
                }
            });
        }
        pending.add(entry);
    }

    private void writeEntries(List<AuditLog> entries) {
        try {
            writer.write(entries);
        } catch (RuntimeException e) {
            // The change itself is already committed and correct. Losing its
            // audit row must not undo it, so this is reported and swallowed —
            // with enough detail to reconstruct the row by hand.
            log.error("Failed to write {} audit entries: {}", entries.size(), summarise(entries), e);
        }
    }

    private String summarise(List<AuditLog> entries) {
        return entries.stream()
                .map(e -> "%s %s#%s by %s".formatted(e.getAction(), e.getEntity(), e.getEntityId(), e.getUserId()))
                .toList()
                .toString();
    }

    /**
     * Renders a field map as JSON, small enough for the column.
     *
     * <p>An oversized document is replaced rather than cut: truncated JSON
     * parses as nothing, or worse, as less than it should. A marker says
     * honestly that the detail is gone.
     */
    String toJson(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) return null;
        try {
            String json = objectMapper.writeValueAsString(fields);
            if (json.length() <= MAX_JSON) return json;
            return objectMapper.writeValueAsString(Map.of(
                    "_truncated", "%d fields, too large to store".formatted(fields.size())));
        } catch (Exception e) {
            log.warn("Could not serialise audit fields", e);
            return null;
        }
    }
}
