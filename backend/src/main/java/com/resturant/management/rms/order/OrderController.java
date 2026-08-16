package com.resturant.management.rms.order;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.order.dto.OrderDtos.OpenOrderRequest;
import com.resturant.management.rms.order.dto.OrderDtos.OrderResponse;
import com.resturant.management.rms.order.dto.OrderDtos.UpdateItemsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel an open bill", description = "Frees the table.")
    public ApiResponse<OrderResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Order cancelled", orderService.cancel(id));
    }
}
