package com.resturant.management.rms.order.dto;

import com.resturant.management.rms.order.OrderStatus;
import com.resturant.management.rms.order.PaymentMethod;
import com.resturant.management.rms.order.PaymentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class OrderDtos {

    /**
     * A code to put on the screen.
     *
     * @param payload   what the QR encodes; the client draws this, no image is
     *                  sent over the wire
     * @param expiresAt when the till stops offering it
     * @param verifiable whether settlement can be confirmed automatically. False
     *                  means no Bakong token is configured, so a human has to
     *                  decide the money arrived — and the screen must say so
     *                  rather than implying the bank agreed.
     */
    public record KhqrResponse(
            String payload,
            String amount,
            String currency,
            java.time.LocalDateTime expiresAt,
            boolean verifiable) {}

    /**
     * What came back from asking the bank.
     *
     * @param state PAID, NOT_PAID, UNKNOWN or UNVERIFIABLE
     */
    public record KhqrStatusResponse(
            String state,
            String detail,
            OrderResponse order) {}

    private OrderDtos() {
    }

    /** Open a bill for a table, or return the one already open there. */
    public record OpenOrderRequest(
            @NotNull(message = "{valid.required}") Long tableId,
            @Positive(message = "{valid.min1}") Integer guestCount,
            /** Optional, and usually absent: a bill is opened before anyone asks. */
            Long customerId
    ) {}

    /**
     * Naming the customer on a bill that is already open, which is when it
     * actually happens — a cashier asks for a phone number at the point of
     * paying, not when the table sits down.
     *
     * <p>A null id clears it, for the case where the wrong one was picked.
     */
    public record SetCustomerRequest(Long customerId) {}

    public record OrderItemRequest(
            @NotNull(message = "{valid.required}") Long productId,
            @NotNull(message = "{valid.required}")
            @Positive(message = "{valid.positive}")
            BigDecimal qty,
            /** Which size, when the dish has sizes. Null means the dish itself. */
            Long variantId,
            /**
             * What was asked for on this line. Ids only: the price of each is
             * read from the menu here, never sent by the client —
             * ARCHITECTURE §5 says the server recalculates everything, and a
             * price that arrived in a request is a price somebody could choose.
             */
            List<Long> modifierIds,
            @Size(max = 255) String note
    ) {}

    /**
     * Replaces the whole line-item set. Sending the full basket rather than
     * per-line deltas keeps the POS and the server in step even if a tablet
     * dropped offline mid-order.
     */
    public record UpdateItemsRequest(
            @Valid @NotNull(message = "{valid.required}") List<OrderItemRequest> items,
            @PositiveOrZero(message = "{valid.notNegative}") BigDecimal discount
    ) {}

    public record OrderItemResponse(
            Long id,
            Long productId,
            String productName,
            Long variantId,
            /** The size's name, as it was when this was ordered. */
            String variantName,
            BigDecimal qty,
            /** Includes the variant's price and every modifier's delta. */
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            List<OrderItemModifierResponse> modifiers,
            String note
    ) {}

    public record OrderItemModifierResponse(
            Long id,
            Long modifierId,
            String name,
            BigDecimal priceDelta
    ) {}

    /** Body of {@code POST /api/orders/{id}/pay}. */
    /**
     * Settling a bill, in either of two shapes.
     *
     * <p>A single tender — which is every sale the POS takes today — is
     * {@code paymentMethod} and {@code amountTendered}, unchanged. A split is
     * {@code payments}: what was taken by each method, which is what
     * {@code sale_payment} exists for.
     *
     * <p>Two shapes is a cost, paid here deliberately. The alternative was to
     * make every caller send a one-element list for the case that is almost all
     * of them, and the service sees only one shape either way because
     * {@link #tenders()} is the only way in.
     */
    public record PayRequest(
            PaymentMethod paymentMethod,
            /** Cash handed over. Required for CASH so change can be worked out. */
            @PositiveOrZero(message = "{valid.notNegative}") BigDecimal amountTendered,
            @Valid List<TenderRequest> payments,
            @PositiveOrZero(message = "{valid.notNegative}") BigDecimal discount
    ) {
        /**
         * One of the two shapes, not both and not neither. Without this a
         * request carrying a method *and* a list would quietly settle twice.
         */
        @AssertTrue(message = "{valid.payment.oneShape}")
        public boolean isExactlyOneShape() {
            return (paymentMethod != null) ^ (payments != null && !payments.isEmpty());
        }

        /** What the service works with, whichever shape arrived. */
        public List<TenderRequest> tenders() {
            if (payments != null && !payments.isEmpty()) return payments;
            return List.of(new TenderRequest(paymentMethod, null, amountTendered, null));
        }
    }

    /**
     * One tender inside a split.
     *
     * @param amount what this covers. Null means "the rest of the bill", which
     *               is what a single tender always means and saves a cashier
     *               re-typing a total the till already knows.
     */
    public record TenderRequest(
            @NotNull(message = "{valid.required}") PaymentMethod method,
            @Positive(message = "{valid.positive}") BigDecimal amount,
            @PositiveOrZero(message = "{valid.notNegative}") BigDecimal tendered,
            @Size(max = 100) String reference
    ) {}

    /** One payment against a bill, as a receipt or a report sees it. */
    public record PaymentResponse(
            Long id,
            PaymentMethod method,
            BigDecimal amount,
            BigDecimal amountKhr,
            BigDecimal tendered,
            BigDecimal changeAmount,
            String reference,
            PaymentStatus status,
            LocalDateTime createdAt
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
            Long customerId,
            String customerName,
            Integer guestCount,
            List<OrderItemResponse> items,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal vatRate,
            BigDecimal vatAmount,
            BigDecimal total,
            BigDecimal totalKhr,
            /** The rate totalKhr was worked out at, stamped when it was. */
            BigDecimal fxRateKhr,
            /**
             * Every payment against this bill, including a code that was shown
             * and never paid. The three fields below are derived from it for
             * the screens that show one line.
             */
            List<PaymentResponse> payments,
            /** The method, when there is exactly one — null when a bill was split. */
            PaymentMethod paymentMethod,
            BigDecimal amountTendered,
            BigDecimal changeAmount,
            OrderStatus status,
            LocalDateTime createdAt,
            LocalDateTime paidAt
    ) {}
}
