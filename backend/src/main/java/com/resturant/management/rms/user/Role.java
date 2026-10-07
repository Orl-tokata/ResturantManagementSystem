package com.resturant.management.rms.user;

/**
 * Extends the NIEI-Y4 {@code USER, ADMIN} pair to the four roles the prototype
 * needs. Existing {@code USER} rows migrate to {@link #CASHIER}.
 */
public enum Role {
    ADMIN,
    /**
     * Added by V17. {@code @PreAuthorize} has referred to it since P3 and the
     * role CHECK refused it until then, so it existed in the application's
     * vocabulary and nowhere else.
     */
    MANAGER,
    CASHIER,
    WAITER,
    CHEF;

    /**
     * Whether this role may be given a login.
     *
     * <p>Two of the five can. The other three are job titles on an HR record:
     * a chef has a row in {@code staff}, not a password. The distinction was
     * missing, and it mattered — the till, payments, cash shifts and refunds
     * are guarded by nothing stronger than "signed in", so a CHEF account
     * could ring up sales and hand money back. Nothing below ADMIN had ever
     * been told apart.
     *
     * <p>MANAGER sits with them for now. {@code @PreAuthorize} has granted it
     * admin work since P3 and V17 widened the role CHECK to accept it, but no
     * screen can create one and the admin shell turns it away, so the grants
     * are unreachable. They are left in place for when the role is finished;
     * until then this is the one place that decides, and it says no.
     */
    public boolean canSignIn() {
        return this == ADMIN || this == CASHIER;
    }
}
