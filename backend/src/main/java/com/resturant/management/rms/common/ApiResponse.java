package com.resturant.management.rms.common;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Standard response envelope for every endpoint.
 *
 * <p>Shape is kept identical to the NIEI-Y4 project so existing clients keep working:
 * {@code { status, message, data, timestamp } }.
 *
 * <p>Failures additionally carry {@code code} — the message key the error was
 * raised with, before {@link com.resturant.management.rms.common.exception.GlobalExceptionHandler}
 * turned it into a sentence. The key was always computed and then dropped at
 * the boundary, which left a client with nothing to branch on but translated
 * prose: "table already occupied" and "insufficient stock" are both a 409, and
 * the text of either changes with the request's language. {@code code} does not.
 *
 * <p>It is the message key itself rather than a parallel numbering scheme,
 * because a second catalogue of identifiers is a second thing to keep in step.
 *
 * <p>Successes leave it null, and {@code non_null} inclusion omits it entirely,
 * so every existing success payload is unchanged.
 */
@Getter
public class ApiResponse<T> {

    private final int status;
    private final String code;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    public ApiResponse(int status, String message, T data) {
        this(status, null, message, data);
    }

    public ApiResponse(int status, String code, String message, T data) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "OK", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "Created", data);
    }

    /**
     * A failure, naming both the machine-readable key and the sentence a person
     * reads.
     *
     * <p>There is deliberately no overload without {@code code}: a handler that
     * forgets one should fail to compile, because an error nothing can branch
     * on is exactly the gap this field exists to close.
     */
    public static <T> ApiResponse<T> error(int status, String code, String message) {
        return new ApiResponse<>(status, code, message, null);
    }
}
