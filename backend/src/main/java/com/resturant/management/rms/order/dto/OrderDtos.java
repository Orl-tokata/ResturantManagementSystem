package com.resturant.management.rms.order.dto;

import com.resturant.management.rms.order.OrderStatus;
import com.resturant.management.rms.order.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    /** Open a bill for a table, or return the one already open there. */
    public record OpenOrderRequest(
            @NotNull(message = "is required") Long tableId,
            @Positive(message = "must be at least 1") Integer guestCount
    ) {}

    public record OrderItemRequest(
            @NotNull(message = "is required") Long productId,
            @NotNull(message = "is required")
            @Positive(message = "must be greater than zero")
            BigDecimal qty,
            @Size(max = 255) String note
    ) {}

    /**
     * Replaces the whole line-item set. Sending the full basket rather than
     * per-line deltas keeps the POS and the server in step even if a tablet
     * dropped offline mid-order.
     */
    public record UpdateItemsRequest(
            @Valid @NotNull(message = "is required") List<OrderItemRequest> items,
            @PositiveOrZero(message = "cannot be negative") BigDecimal discount
    ) {}

    public record OrderItemResponse(
            Long id,
            Long productId,
            String productName,
            BigDecimal qty,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            String note
    ) {}

    /** Body of {@code POST /api/orders/{id}/pay}. */
    public record PayRequest(
            @NotNull(message = "is required") PaymentMethod paymentMethod,
            /** Cash handed over. Required for CASH so change can be worked out. */
            @PositiveOrZero(message = "cannot be negative") BigDecimal amountTendered,
            @PositiveOrZero(message = "cannot be negative") BigDecimal discount
    ) {}

    /** The four tiles above the order-history table. */
    public record HistorySummary(
            BigDecimal totalSales,
            long paidCount,
            BigDecimal averageSale,
            long cancelledCount,
            long totalCount
    ) {}

    /** Everything a printed receipt needs, so the frontend makes one request. */
    public record ReceiptResponse(
            String restaurantName,
            String restaurantNameEn,
            String address,
            String phone,
            OrderResponse order
    ) {}

    public record OrderResponse(
            Long id,
            String invoiceNo,
            Long tableId,
            String tableName,
            Long cashierId,
            String cashierName,
            Integer guestCount,
            List<OrderItemResponse> items,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal vatRate,
            BigDecimal vatAmount,
            BigDecimal total,
            BigDecimal totalKhr,
            PaymentMethod paymentMethod,
            BigDecimal amountTendered,
            BigDecimal changeAmount,
            OrderStatus status,
            LocalDateTime createdAt,
            LocalDateTime paidAt
    ) {}
}
