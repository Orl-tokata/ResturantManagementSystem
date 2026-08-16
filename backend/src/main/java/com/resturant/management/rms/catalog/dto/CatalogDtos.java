package com.resturant.management.rms.catalog.dto;

import com.resturant.management.rms.common.RecordStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Request and response payloads for categories and products. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    /* ---- Category -------------------------------------------------------- */

    public record CategoryRequest(
            @NotBlank(message = "is required") @Size(max = 100) String name,
            @Size(max = 100) String nameEn,
            @Size(max = 20) String icon,
            @PositiveOrZero(message = "cannot be negative") Integer sortOrder,
            RecordStatus status
    ) {}

    public record CategoryResponse(
            Long id,
            String name,
            String nameEn,
            String icon,
            Integer sortOrder,
            RecordStatus status,
            long productCount
    ) {}

    /* ---- Product --------------------------------------------------------- */

    public record ProductRequest(
            @NotBlank(message = "is required") @Size(max = 150) String name,
            @Size(max = 150) String nameEn,
            @NotNull(message = "is required") Long categoryId,
            @NotNull(message = "is required")
            @PositiveOrZero(message = "cannot be negative")
            @Digits(integer = 10, fraction = 2, message = "allows at most 2 decimal places")
            BigDecimal price,
            @PositiveOrZero(message = "cannot be negative")
            @Digits(integer = 10, fraction = 2, message = "allows at most 2 decimal places")
            BigDecimal cost,
            @PositiveOrZero(message = "cannot be negative") BigDecimal stockQty,
            @Size(max = 255) String imageUrl,
            @Size(max = 1000) String description,
            RecordStatus status
    ) {}

    public record ProductResponse(
            Long id,
            String name,
            String nameEn,
            Long categoryId,
            String categoryName,
            BigDecimal price,
            BigDecimal cost,
            BigDecimal stockQty,
            String imageUrl,
            String description,
            RecordStatus status
    ) {}
}
