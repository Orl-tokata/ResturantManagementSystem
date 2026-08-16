package com.resturant.management.rms.common;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Standard response envelope for every endpoint.
 *
 * <p>Shape is kept identical to the NIEI-Y4 project so existing clients keep working:
 * {@code { status, message, data, timestamp } }.
 */
@Getter
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    public ApiResponse(int status, String message, T data) {
        this.status = status;
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

    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }
}
