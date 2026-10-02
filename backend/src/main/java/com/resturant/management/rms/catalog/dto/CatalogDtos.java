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
            @Size(max = 16) String icon,
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
            String icon,
            /**
             * Filename of the product's photograph, or null.
             *
             * <p>Not a URL: the client builds one, so the API does not have to
             * know where it is being served from. Null means the icon is what
             * to show.
             */
            String imageFile,
            String description,
            RecordStatus status,
            /**
             * Whether tapping this on the till has to ask something first.
             *
             * <p>A flag rather than the sizes and questions themselves: the
             * POS draws a grid of every dish and most of them ask nothing, so
             * sending the detail would be paying for it on every tile. The
             * chooser fetches what it needs when it opens.
             */
            boolean hasOptions
    ) {}

    /* ---- Variants and modifiers (API §6.5) -------------------------------- */

    public record VariantRequest(
            @NotBlank(message = "{valid.required}") @Size(max = 100) String name,
            @Size(max = 100) String nameEn,
            @NotNull(message = "{valid.required}")
            @PositiveOrZero(message = "{valid.notNegative}")
            BigDecimal price,
            @PositiveOrZero(message = "{valid.notNegative}") BigDecimal cost,
            @Size(max = 50) String sku,
            @Size(max = 50) String barcode,
            Integer sortOrder
    ) {}

    public record VariantResponse(
            Long id,
            Long productId,
            String name,
            String nameEn,
            BigDecimal price,
            BigDecimal cost,
            String sku,
            String barcode,
            Integer sortOrder
    ) {}

    public record ModifierRequest(
            @NotBlank(message = "{valid.required}") @Size(max = 100) String name,
            @Size(max = 100) String nameEn,
            /** Signed: free, dearer, or a reduction for leaving something out. */
            BigDecimal priceDelta,
            Integer sortOrder
    ) {}

    /**
     * A question and its answers, saved together.
     *
     * <p>One call rather than a group endpoint and a modifier endpoint: a
     * group is meaningless without its options, and editing them apart means a
     * half-saved question that the POS would still have to render.
     */
    public record ModifierGroupRequest(
            @NotBlank(message = "{valid.required}") @Size(max = 100) String name,
            @Size(max = 100) String nameEn,
            @PositiveOrZero(message = "{valid.notNegative}") Integer minSelect,
            @Positive(message = "{valid.positive}") Integer maxSelect,
            Integer sortOrder,
            @NotEmpty(message = "{valid.atLeastOneLine}")
            @jakarta.validation.Valid java.util.List<ModifierRequest> modifiers
    ) {}

    public record ModifierResponse(
            Long id,
            String name,
            String nameEn,
            BigDecimal priceDelta,
            Integer sortOrder
    ) {}

    public record ModifierGroupResponse(
            Long id,
            String name,
            String nameEn,
            Integer minSelect,
            Integer maxSelect,
            Integer sortOrder,
            /** True when the customer has to answer before the line is valid. */
            boolean required,
            java.util.List<ModifierResponse> modifiers
    ) {}
}
