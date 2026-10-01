package com.resturant.management.rms.stock.dto;

import com.resturant.management.rms.stock.MovementType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class StockDtos {

    private StockDtos() {
    }

    public record StockItemRequest(
            @NotBlank(message = "{valid.required}") @Size(max = 150) String name,
            @NotBlank(message = "{valid.required}") @Size(max = 30) String unit,
            @PositiveOrZero(message = "{valid.notNegative}") BigDecimal qty,
            @PositiveOrZero(message = "{valid.notNegative}") BigDecimal minQty,
            @PositiveOrZero(message = "{valid.notNegative}") BigDecimal unitCost
    ) {}

    public record StockItemResponse(
            Long id,
            String name,
            String unit,
            BigDecimal qty,
            BigDecimal minQty,
            BigDecimal unitCost,
            BigDecimal value,
            boolean lowStock,
            boolean outOfStock
    ) {}

    /**
     * A stock correction. {@code qty} is always positive — the direction comes
     * from {@code type}, so a caller cannot accidentally add by sending a
     * negative number to an OUT movement.
     */
    public record AdjustRequest(
            @NotNull(message = "{valid.required}") MovementType type,
            @NotNull(message = "{valid.required}")
            @Positive(message = "{valid.positive}")
            BigDecimal qty,
            @Size(max = 500) String reason
    ) {}

    /**
     * One line of the ledger.
     *
     * <p>A row is about a product or a stock item, never both, so one pair of
     * fields is always null — which is the shape of the table rather than an
     * oversight.
     */
    public record MovementResponse(
            Long id,
            Long stockItemId,
            String stockItemName,
            Long productId,
            String productName,
            MovementType type,
            BigDecimal qty,
            /** True when this added stock; the quantity itself is always positive. */
            boolean increase,
            /**
             * The balance immediately after this movement.
             *
             * <p>Null on rows written before the ledger existed — the balance
             * before them was never recorded, and a guess here would read as a
             * record. See V11.
             */
            BigDecimal balanceAfter,
            /** What caused it, e.g. ORDER, and that document's id. */
            String refType,
            Long refId,
            String reason,
            String createdBy,
            LocalDateTime createdAt
    ) {}

    /** Tiles above the stock table. */
    public record StockSummary(
            long totalItems,
            BigDecimal stockValue,
            long lowStockCount,
            long outOfStockCount
    ) {}
}
