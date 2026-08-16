package com.resturant.management.rms.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Flattened page payload — avoids leaking Spring Data's {@code Page} JSON shape,
 * which is unstable across versions.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
