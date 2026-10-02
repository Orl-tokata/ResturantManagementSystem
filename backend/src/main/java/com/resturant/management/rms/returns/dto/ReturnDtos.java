package com.resturant.management.rms.returns.dto;

import com.resturant.management.rms.order.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ReturnDtos {

    private ReturnDtos() {
    }

    /**
     * What is still returnable on one line of a sale.
     *
     * <p>The server does this subtraction, always. No database constraint can
     * stop a line of two being returned three times across two documents
     * (ERD §3.7), so the remaining quantity is computed where the write
     * happens and the client is told the answer rather than working it out.
     */
    public record ReturnableLine(
            Long orderItemId,
            Long productId,
            String productName,
            BigDecimal soldQty,
            BigDecimal returnedQty,
            BigDecimal remainingQty,
            BigDecimal unitPrice
    ) {}

    /** The sale, as the returns screen needs to see it. */
    public record ReturnableOrder(
            Long orderId,
            String invoiceNo,
            LocalDateTime paidAt,
            BigDecimal total,
            /** What the bill was settled by, which is what a refund defaults to. */
            PaymentMethod originalMethod,
            boolean anythingLeft,
            List<ReturnableLine> lines
    ) {}

    public record ReturnLineRequest(
            @NotNull(message = "{valid.required}") Long orderItemId,
            @NotNull(message = "{valid.required}")
            @Positive(message = "{valid.positive}")
            BigDecimal qty
    ) {}

    public record CreateReturnRequest(
            @NotNull(message = "{valid.required}") Long orderId,
            @NotEmpty(message = "{valid.atLeastOneLine}")
            @Valid List<ReturnLineRequest> lines,
            /** Mandatory. A refund nobody explained is the entry that cannot be accounted for. */
            @NotBlank(message = "{valid.required}")
            @Size(max = 500) String reason,
            /** Null means "however it was paid", which is the right answer almost always. */
            PaymentMethod refundMethod
    ) {}

    public record ReturnItemResponse(
            Long id,
            Long orderItemId,
            String productName,
            BigDecimal qty,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}

    public record ReturnResponse(
            Long id,
            String returnNo,
            Long orderId,
            String invoiceNo,
            BigDecimal total,
            PaymentMethod refundMethod,
            String reason,
            String createdBy,
            LocalDateTime createdAt,
            /** Who agreed to it, when the amount meant somebody had to. */
            String approvedBy,
            Long shiftId,
            List<ReturnItemResponse> items
    ) {}
}
