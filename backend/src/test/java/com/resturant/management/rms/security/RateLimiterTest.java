package com.resturant.management.rms.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The algorithm on its own, with time under the test's control.
 *
 * <p>Driving a clock rather than sleeping is what makes the refill behaviour
 * testable at all: proving a one-minute window refills would otherwise mean a
 * one-minute test.
 */
class RateLimiterTest {

    /** A clock the test moves by hand. */
    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }

        void advance(Duration by) { now = now.plus(by); }
    }

    private static final Duration MINUTE = Duration.ofMinutes(1);

    private MovableClock clock;
    private RateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MovableClock();
        limiter = new RateLimiter(clock);
    }

    @Test
    @DisplayName("the first requests up to the limit are allowed, the next is not")
    void allowsUpToTheLimit() {
        for (int i = 1; i <= 5; i++) {
            assertThat(limiter.tryConsume("a", 5, MINUTE).allowed())
                    .as("request %d of 5", i).isTrue();
        }
        assertThat(limiter.tryConsume("a", 5, MINUTE).allowed()).isFalse();
    }

    @Test
    @DisplayName("callers are independent")
    void keysAreSeparate() {
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("a", 5, MINUTE);
        }
        assertThat(limiter.tryConsume("a", 5, MINUTE).allowed()).isFalse();
        assertThat(limiter.tryConsume("b", 5, MINUTE).allowed())
                .as("a different caller should be unaffected").isTrue();
    }

    @Test
    @DisplayName("tokens come back gradually, not all at once")
    void refillsContinuously() {
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("a", 5, MINUTE);
        }
        assertThat(limiter.tryConsume("a", 5, MINUTE).allowed()).isFalse();

        // One fifth of the window is one token.
        clock.advance(Duration.ofSeconds(12));
        assertThat(limiter.tryConsume("a", 5, MINUTE).allowed()).isTrue();
        assertThat(limiter.tryConsume("a", 5, MINUTE).allowed())
                .as("only one token had refilled").isFalse();

        clock.advance(MINUTE);
        for (int i = 1; i <= 5; i++) {
            assertThat(limiter.tryConsume("a", 5, MINUTE).allowed())
                    .as("after a full window, the whole allowance is back (%d)", i).isTrue();
        }
    }

    /**
     * The reason for a token bucket rather than a fixed window. A window lets a
     * caller spend everything at the end of one and everything again at the
     * start of the next — 10 requests in an instant against a limit of 5.
     */
    @Test
    @DisplayName("no double allowance across a window boundary")
    void noBurstAtTheBoundary() {
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("a", 5, MINUTE);
        }
        // A hair before the window is up, almost everything should still refuse.
        clock.advance(Duration.ofSeconds(59));

        int allowed = 0;
        for (int i = 0; i < 10; i++) {
            if (limiter.tryConsume("a", 5, MINUTE).allowed()) allowed++;
        }
        assertThat(allowed)
                .as("a fixed window would have allowed all 10 here")
                .isLessThanOrEqualTo(5);
    }

    @Test
    @DisplayName("a refusal says how long to wait, and the wait is enough")
    void retryAfterIsUsable() {
        for (int i = 0; i < 5; i++) {
            limiter.tryConsume("a", 5, MINUTE);
        }
        RateLimiter.Decision refused = limiter.tryConsume("a", 5, MINUTE);

        assertThat(refused.allowed()).isFalse();
        assertThat(refused.retryAfterSeconds()).isBetween(1L, 60L);

        clock.advance(Duration.ofSeconds(refused.retryAfterSeconds()));
        assertThat(limiter.tryConsume("a", 5, MINUTE).allowed())
                .as("waiting the advertised time should actually work")
                .isTrue();
    }

    /**
     * Two threads must not read the same token count and both spend it. Without
     * the atomic compute() this over-allows, which is the whole failure mode a
     * rate limiter has to avoid.
     */
    @Test
    @DisplayName("concurrent callers never exceed the limit in total")
    void isThreadSafe() throws Exception {
        int threads = 16;
        int each = 50;
        int limit = 100;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        List<Runnable> jobs = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            jobs.add(() -> {
                try {
                    start.await();
                    for (int i = 0; i < each; i++) {
                        if (limiter.tryConsume("shared", limit, Duration.ofHours(1)).allowed()) {
                            allowed.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        jobs.forEach(pool::submit);
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // The clock never moves, so nothing refills: exactly `limit` may pass.
        assertThat(allowed.get())
                .as("%d threads x %d requests against a limit of %d", threads, each, limit)
                .isEqualTo(limit);
    }
}
