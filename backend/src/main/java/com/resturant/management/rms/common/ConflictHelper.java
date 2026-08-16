package com.resturant.management.rms.common;

import com.resturant.management.rms.common.exception.ConflictException;

/** Consistent wording for the two conflicts every CRUD screen can hit. */
public final class ConflictHelper {

    private ConflictHelper() {
    }

    public static ConflictException duplicate(String entity, String field, String value) {
        return new ConflictException("%s with %s '%s' already exists".formatted(entity, field, value));
    }

    public static ConflictException inUse(String entity, String name, long count, String usedBy) {
        return new ConflictException(
                "Cannot delete %s '%s': it is still used by %d %s".formatted(entity, name, count, usedBy));
    }
}
