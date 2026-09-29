package com.resturant.management.rms.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Claims a key before a write runs, and records the reply after.
 *
 * <p>Every method commits in its own transaction. That is not decoration: a
 * claim has to be visible to a second request <em>while the first is still
 * being handled</em>, which it would not be if it shared the request's
 * transaction and committed at the end with everything else.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    /** Matches {@code response_body VARCHAR(8000)} in V6. */
    static final int MAX_BODY = 8000;

    private final IdempotencyKeyRepository repository;

    public enum Outcome {
        /** First time seen — handle the request. */
        PROCEED,
        /** Seen and finished — return the stored reply. */
        REPLAY,
        /** Seen and still running elsewhere. */
        IN_PROGRESS,
        /** Seen, but claimed for a different endpoint. */
        MISMATCH,
        /** Seen and finished, but the reply was not storable. */
        UNREPLAYABLE
    }

    public record Claim(Outcome outcome, Integer status, String body) {
        static Claim of(Outcome outcome) {
            return new Claim(outcome, null, null);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Claim claim(String key, String method, String path, String userId) {
        Optional<IdempotencyKey> existing = repository.findByKeyValue(key);
        if (existing.isPresent()) {
            return decide(existing.get(), method, path);
        }

        try {
            repository.saveAndFlush(IdempotencyKey.builder()
                    .keyValue(key)
                    .method(method)
                    .path(path)
                    .userId(userId)
                    .state(IdempotencyKey.State.IN_PROGRESS)
                    .createdAt(LocalDateTime.now())
                    .build());
            return Claim.of(Outcome.PROCEED);

        } catch (DataIntegrityViolationException e) {
            /*
             * Two requests arrived together and the other one won the insert.
             *
             * Reported as IN_PROGRESS without re-reading: the losing insert has
             * already poisoned this persistence context, and a read here is not
             * worth a second transaction. If the winner has in fact finished,
             * the caller's retry gets the replay on the next attempt — one
             * extra round trip in a race that is itself rare.
             */
            log.debug("Idempotency key {} was claimed concurrently", key);
            return Claim.of(Outcome.IN_PROGRESS);
        }
    }

    private Claim decide(IdempotencyKey row, String method, String path) {
        if (!row.matches(method, path)) {
            // Answering with the other endpoint's reply would be worse than
            // refusing: the caller would believe something happened that did not.
            return Claim.of(Outcome.MISMATCH);
        }
        if (row.getState() == IdempotencyKey.State.IN_PROGRESS) {
            return Claim.of(Outcome.IN_PROGRESS);
        }
        if (!row.isReplayable()) {
            return Claim.of(Outcome.UNREPLAYABLE);
        }
        return new Claim(Outcome.REPLAY, row.getResponseStatus(), row.getResponseBody());
    }

    /**
     * Stores the reply so a repeat of the request returns it.
     *
     * <p>A body that will not fit the column is dropped rather than cut short.
     * Replaying truncated JSON would hand a client something that parses as
     * nothing, or worse, parses as less than it should.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(String key, int status, String body) {
        repository.findByKeyValue(key).ifPresent(row -> {
            boolean storable = body != null && body.length() <= MAX_BODY;
            if (!storable) {
                log.warn("Reply to {} {} is {} chars and will not be replayable",
                        row.getMethod(), row.getPath(), body == null ? 0 : body.length());
            }
            row.setState(IdempotencyKey.State.COMPLETED);
            row.setResponseStatus(status);
            row.setResponseBody(storable ? body : null);
            row.setCompletedAt(LocalDateTime.now());
        });
    }

    /**
     * Frees the key after a failed request.
     *
     * <p>Deliberately not stored as a replayable failure. A cashier who is told
     * "amount tendered is less than the total" fixes it and sends the same
     * intent again; if the rejection were cached, the corrected request would
     * be answered with the original complaint. It also stops one transient 500
     * from making a key permanently unusable.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String key) {
        repository.findByKeyValue(key).ifPresent(repository::delete);
    }
}
