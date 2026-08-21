package com.resturant.management.rms.common;

import com.resturant.management.rms.common.exception.ConflictException;
import com.resturant.management.rms.common.exception.LocalizedArg;

/** Consistent wording for the two conflicts every CRUD screen can hit. */
public final class ConflictHelper {

    private ConflictHelper() {
    }

    /**
     * @param entityKey message key for the entity name, e.g. "entity.supplier"
     * @param fieldKey  message key for the field name, e.g. "field.code"
     */
    public static ConflictException duplicate(String entityKey, String fieldKey, String value) {
        return new ConflictException(
                "error.duplicate", new LocalizedArg(entityKey), new LocalizedArg(fieldKey), value);
    }

    /**
     * @param usedByKey message key for what still references it, e.g.
     *                  "usedBy.products" — a bare English noun here is the
     *                  easiest way to leave half a sentence untranslated
     */
    public static ConflictException inUse(String entityKey, String name, long count, String usedByKey) {
        return new ConflictException(
                "error.inUse", new LocalizedArg(entityKey), name, count, new LocalizedArg(usedByKey));
    }
}
