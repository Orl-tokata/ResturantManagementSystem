package com.resturant.management.rms.branch;

/**
 * Which branch the work in hand belongs to.
 *
 * <p>A thread-local, set once per request from the signed token and cleared in
 * a {@code finally}. It is read by {@link BranchTenantResolver}, which is what
 * actually puts the branch into every query, so nothing in a service or a
 * repository has to remember to.
 *
 * <p>The alternative was a {@code branchId} parameter threaded through every
 * repository method. Thirty-two methods, and the one that is forgotten shows
 * another shop's takings to whoever asks — API §3 names this as a mistake this
 * system has already made once, with roles. A filter that cannot be forgotten
 * is worth the indirection.
 *
 * <p>Any work that is not a request — startup, a scheduled job, a migration
 * check — sees {@link #DEFAULT_BRANCH}. That is the original shop, which is
 * also the only branch such work can sensibly mean.
 */
public final class BranchContext {

    /** The branch V17 backfilled everything to. */
    public static final Long DEFAULT_BRANCH = 1L;

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private BranchContext() {
    }

    public static void set(Long branchId) {
        CURRENT.set(branchId);
    }

    public static Long get() {
        Long branchId = CURRENT.get();
        return branchId != null ? branchId : DEFAULT_BRANCH;
    }

    /** True when a request actually established one, rather than falling back. */
    public static boolean isSet() {
        return CURRENT.get() != null;
    }

    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Runs something as another branch, then puts the previous one back.
     *
     * <p>For the few places that legitimately cross branches: an owner's
     * report over two shops, and the tests that prove one shop cannot see the
     * other's rows.
     */
    public static <T> T as(Long branchId, java.util.function.Supplier<T> work) {
        Long previous = CURRENT.get();
        CURRENT.set(branchId);
        try {
            return work.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
