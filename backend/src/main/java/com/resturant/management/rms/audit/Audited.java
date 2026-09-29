package com.resturant.management.rms.audit;

import java.lang.annotation.*;

/**
 * Marks an entity whose changes are worth keeping a history of.
 *
 * <p>Opt-in rather than everything. Orders, order lines and stock movements are
 * high-volume and already carry their own history — auditing them would bury
 * the rows someone actually goes looking for, which are the quiet edits to
 * master data: a price, a VAT rate, a role, an exchange rate.
 *
 * <p>Two ways to leave a field out, kept apart on purpose:
 *
 * <ul>
 *   <li>{@code redact} — must never be written. A password hash is still a
 *       credential, and an audit trail is read by more people than the table it
 *       came from. Removing one of these is a security decision.</li>
 *   <li>{@code ignore} — could be written, but is not worth it. A dining
 *       table's status flips on every order; recording it would bury the
 *       configuration changes someone actually opens this log to find.
 *       Removing one of these is only a judgement about noise.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

    /** Name in the log. Defaults to the simple class name. */
    String value() default "";

    /** Secrets. Never recorded, in either the before or the after state. */
    String[] redact() default {};

    /** High-churn fields, left out to keep the log readable. */
    String[] ignore() default {};
}
