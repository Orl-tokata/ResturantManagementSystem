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
            @NotBlank(message = "{valid.required}") @Size(max = 100) String name,
            @Size(max = 100) String nameEn,
            @Size(max = 20) String icon,
            @PositiveOrZero(message = "{valid.notNegative}") Integer sortOrder,
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
            @NotBlank(message = "{valid.required}") @Size(max = 150) String name,
            @Size(max = 150) String nameEn,
            @NotNull(message = "{valid.required}") Long categoryId,
            @NotNull(message = "{valid.required}")
            @PositiveOrZero(message = "{valid.notNegative}")
            @Digits(integer = 10, fraction = 2, message = "{valid.decimals}")
            BigDecimal price,
            @PositiveOrZero(message = "{valid.notNegative}")
            @Digits(integer = 10, fraction = 2, message = "{valid.decimals}")
            BigDecimal cost,
            @PositiveOrZero(message = "{valid.notNegative}") BigDecimal stockQty,
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
