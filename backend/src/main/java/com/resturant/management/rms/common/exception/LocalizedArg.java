package com.resturant.management.rms.common.exception;

/**
 * Wraps a message argument that is itself a message key.
 *
 * <p>"Category not found: 5" has a translatable noun inside a translatable
 * sentence. Without this the entity name would have to be resolved where the
 * exception is thrown, which is exactly the place that does not know the
 * locale. The handler unwraps these just before formatting.
 */
public record LocalizedArg(String key) {
}
