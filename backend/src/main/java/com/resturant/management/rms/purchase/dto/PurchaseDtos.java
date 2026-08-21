package com.resturant.management.rms.purchase.dto;

import com.resturant.management.rms.purchase.PurchaseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PurchaseDtos {

    private PurchaseDtos() {
    }

    public record PurchaseItemRequest(
            @NotNull(message = "{valid.required}") Long stockItemId,
            @NotNull(message = "{valid.required}")
            @Positive(message = "{valid.positive}") BigDecimal qty,
            @NotNull(message = "{valid.required}")
            @PositiveOrZero(message = "{valid.notNegative}") BigDecimal unitCost
    ) {}

    public record PurchaseRequest(
            @NotNull(message = "{valid.required}") Long supplierId,
            @NotNull(message = "{valid.required}") LocalDate purchaseDate,
            @Valid @NotEmpty(message = "{valid.atLeastOneLine}")
            List<PurchaseItemRequest> items,
            @Size(max = 500) String note
    ) {}

    public record PurchaseItemResponse(
            Long id,
            Long stockItemId,
            String itemName,
            BigDecimal qty,
            BigDecimal unitCost,
            BigDecimal lineTotal
    ) {}

    public record PurchaseResponse(
            Long id,
            String poNo,
            Long supplierId,
            String supplierName,
            LocalDate purchaseDate,
            List<PurchaseItemResponse> items,
            BigDecimal total,
            PurchaseStatus status,
            String note
    ) {}

    /** Tiles above the purchase table. */
    public record PurchaseSummary(
            BigDecimal monthTotal,
            long orderCount,
            long pendingCount,
            BigDecimal payable
    ) {}
}
