package com.resturant.management.rms.security;

import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A token bucket per caller, in memory.
 *
 * <p>Token bucket rather than a fixed window because a window lets a caller
 * spend its whole allowance at the end of one window and again at the start of
 * the next — twice the limit, back to back, which is exactly the burst the
 * limit exists to stop. Tokens refill continuously instead, so the long-run
 * rate is the limit however the requests line up.
 *
 * <p><b>In memory, so per instance.</b> Two instances behind a load balancer
 * would each allow the full limit. That is honest for this app, which runs as
 * one process; a second instance needs a shared counter in Redis or similar,
 * and {@link #tryConsume} is the seam where that would go.
 */
@Slf4j
public class RateLimiter {

    /**
     * Above this many tracked callers, refilled buckets are swept.
     *
     * <p>Unbounded growth would itself be an attack: a flood of forged source
     * addresses costs memory per address. Sweeping is safe because a refilled
     * bucket is indistinguishable from one never seen — dropping it loses
     * nothing, and the next request recreates it.
     */
    private static final int SWEEP_THRESHOLD = 10_000;

    /** How long an untouched bucket is kept even if it is not yet full. */
    private static final Duration STALE_AFTER = Duration.ofHours(1);

    private record Bucket(double tokens, long atMillis) {}

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Allowed, and if not, roughly how long until a token is free again. */
    public record Decision(boolean allowed, long retryAfterSeconds) {
        private static final Decision ALLOWED = new Decision(true, 0);
    }

    /**
     * Takes one token for {@code key}, or reports how long to wait.
     *
     * @param limit  tokens per window, and the largest burst allowed
     * @param window the period over which an empty bucket refills completely
     */
    public Decision tryConsume(String key, int limit, Duration window) {
        long now = clock.millis();
        double millisPerToken = (double) window.toMillis() / limit;
        AtomicReference<Decision> decision = new AtomicReference<>();

        // The decision is made inside compute() rather than derived from the
        // value it returns. Spending leaves a fraction below one whenever a
        // caller is exactly at its limit, so the resulting token count cannot
        // tell "paid its last token" from "could not pay" — only the code that
        // saw the pre-state knows. compute() is atomic per key, which is also
        // what stops two threads reading the same count and both spending it.
        buckets.compute(key, (k, before) -> {
            double tokens = (before == null)
                    ? limit
                    : Math.min(limit, before.tokens() + (now - before.atMillis()) / millisPerToken);

            if (tokens >= 1) {
                decision.set(Decision.ALLOWED);
                return new Bucket(tokens - 1, now);
            }

            long retryMillis = (long) Math.ceil((1 - tokens) * millisPerToken);
            decision.set(new Decision(false, Math.max(1, Math.round(retryMillis / 1000.0))));
            return new Bucket(tokens, now);
        });

        if (buckets.size() > SWEEP_THRESHOLD) {
            sweep(limit);
        }
        return decision.get();
    }

    /** Drops callers whose buckets have refilled; they carry no information. */
    private void sweep(int limit) {
        int before = buckets.size();
        long now = clock.millis();
        buckets.entrySet().removeIf(e -> {
            Bucket b = e.getValue();
            boolean full = b.tokens() >= limit - 1e-9;
            boolean stale = now - b.atMillis() > STALE_AFTER.toMillis();
            return full || stale;
        });
        log.debug("Rate-limit sweep: {} tracked callers -> {}", before, buckets.size());
    }

    /** Forgets every caller. For tests, and for an operator clearing a mistake. */
    public void reset() {
        buckets.clear();
    }
}
