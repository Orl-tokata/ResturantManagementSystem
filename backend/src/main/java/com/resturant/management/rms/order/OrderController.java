package com.resturant.management.rms.order;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import com.resturant.management.rms.order.dto.OrderDtos.HistorySummary;
import com.resturant.management.rms.order.dto.OrderDtos.KhqrResponse;
import com.resturant.management.rms.order.dto.OrderDtos.KhqrStatusResponse;
import com.resturant.management.rms.order.dto.OrderDtos.OpenOrderRequest;
import com.resturant.management.rms.order.dto.OrderDtos.OrderResponse;
import com.resturant.management.rms.order.dto.OrderDtos.PayRequest;
import com.resturant.management.rms.order.dto.OrderDtos.ReceiptResponse;
import com.resturant.management.rms.order.dto.OrderDtos.UpdateItemsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Point of sale — open bills and their line items")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Open a bill at a table",
            description = "Returns the bill already open at that table if there is one, "
                        + "so tapping twice cannot create two competing bills.")
    public ResponseEntity<ApiResponse<OrderResponse>> open(
            @Valid @RequestBody OpenOrderRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        OrderResponse order = orderService.openOrReuse(request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(order));
    }

    @GetMapping
    @Operation(summary = "Order history (paged)",
            description = "Filter by invoice number, status and an inclusive date range. "
                        + "Dates cover whole days, so `to` includes bills taken that day.")
    public ApiResponse<PageResponse<OrderResponse>> history(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100));
        return ApiResponse.ok(PageResponse.from(
                orderService.history(search, status, from, to, pageable)));
    }

    @GetMapping("/summary")
    @Operation(summary = "History totals for a date range",
            description = "Backs the four tiles above the history table.")
    public ApiResponse<HistorySummary> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(orderService.historySummary(from, to));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one order")
    public ApiResponse<OrderResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(orderService.get(id));
    }

    @GetMapping("/open")
    @Operation(summary = "The open bill at a table",
            description = "404 when the table has no open bill. Used when the POS reloads.")
    public ApiResponse<OrderResponse> openForTable(@RequestParam Long tableId) {
        return ApiResponse.ok(orderService.openForTable(tableId));
    }

    @PutMapping("/{id}/items")
    @Operation(summary = "Replace the line items",
            description = "Send the whole basket. Unit prices are taken from the catalog "
                        + "at this moment and frozen onto the bill.")
    public ApiResponse<OrderResponse> replaceItems(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateItemsRequest request) {
        return ApiResponse.ok("Order updated", orderService.replaceItems(id, request));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Settle a bill",
            description = "One transaction: mark PAID, record tender and change, decrement stock, "
                        + "free the table. Cash requires amountTendered ≥ total.")
    public ApiResponse<OrderResponse> pay(@PathVariable Long id,
                                          @Valid @RequestBody PayRequest request) {
        return ApiResponse.ok("Payment accepted", orderService.pay(id, request));
    }

    /* ---- KHQR ---------------------------------------------------------- */

    @PostMapping("/{id}/khqr")
    @Operation(summary = "Show a KHQR code for a bill",
            description = "Parks the order in AWAITING_PAYMENT and returns the payload to draw. "
                        + "Calling it again while a code is live returns the same one, so a "
                        + "refreshed till cannot orphan a customer who already scanned.")
    public ApiResponse<KhqrResponse> khqr(@PathVariable Long id) {
        return ApiResponse.ok(orderService.startKhqrPayment(id));
    }

    @GetMapping("/{id}/khqr")
    @Operation(summary = "Ask the bank whether it was paid",
            description = "Returns PAID, NOT_PAID, UNKNOWN, UNVERIFIABLE or EXPIRED. Only PAID "
                        + "changes the order; an unreachable bank leaves it untouched.")
    public ApiResponse<KhqrStatusResponse> khqrStatus(@PathVariable Long id) {
        return ApiResponse.ok(orderService.checkKhqrPayment(id));
    }

    @DeleteMapping("/{id}/khqr")
    @Operation(summary = "Give up on an outstanding code",
            description = "Returns the bill to OPEN so it can be settled another way.")
    public ApiResponse<OrderResponse> abandonKhqr(@PathVariable Long id) {
        return ApiResponse.ok("Payment abandoned", orderService.abandonKhqrPayment(id));
    }

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Receipt projection",
            description = "The order plus the restaurant header, in one request.")
    public ApiResponse<ReceiptResponse> receipt(@PathVariable Long id) {
        return ApiResponse.ok(orderService.receipt(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an open bill", description = "Frees the table.")
    public ApiResponse<OrderResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Order cancelled", orderService.cancel(id));
    }
}
