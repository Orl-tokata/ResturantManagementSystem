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
            @NotNull(message = "is required") Long stockItemId,
            @NotNull(message = "is required")
            @Positive(message = "must be greater than zero") BigDecimal qty,
            @NotNull(message = "is required")
            @PositiveOrZero(message = "cannot be negative") BigDecimal unitCost
    ) {}

    public record PurchaseRequest(
            @NotNull(message = "is required") Long supplierId,
            @NotNull(message = "is required") LocalDate purchaseDate,
            @Valid @NotEmpty(message = "must contain at least one line")
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
