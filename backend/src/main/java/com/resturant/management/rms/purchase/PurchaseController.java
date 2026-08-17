package com.resturant.management.rms.purchase;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import com.resturant.management.rms.purchase.dto.PurchaseDtos.PurchaseRequest;
import com.resturant.management.rms.purchase.dto.PurchaseDtos.PurchaseResponse;
import com.resturant.management.rms.purchase.dto.PurchaseDtos.PurchaseSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Purchases", description = "Purchase orders and goods receipt (ADMIN only)")
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    @Operation(summary = "List purchase orders (paged)")
    public ApiResponse<PageResponse<PurchaseResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PurchaseStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100));
        return ApiResponse.ok(PageResponse.from(purchaseService.search(search, status, pageable)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Purchase totals for a date range",
            description = "Defaults to the current month. Also reports total payable across suppliers.")
    public ApiResponse<PurchaseSummary> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(purchaseService.summary(from, to));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one purchase order")
    public ApiResponse<PurchaseResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(purchaseService.get(id));
    }

    @PostMapping
    @Operation(summary = "Raise a purchase order",
            description = "Created PENDING. Stock is untouched until the goods are received.")
    public ResponseEntity<ApiResponse<PurchaseResponse>> create(
            @Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(purchaseService.create(request)));
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Receive the goods",
            description = "One transaction: increase every line's stock, write an IN movement for "
                        + "each, refresh unit costs, and add the total to the supplier's payable.")
    public ApiResponse<PurchaseResponse> receive(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok("Goods received", purchaseService.receive(id, principal.getUsername()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a pending order",
            description = "400 once received — cancelling then would overstate the stock.")
    public ApiResponse<PurchaseResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok("Purchase cancelled", purchaseService.cancel(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a purchase order", description = "400 once received.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        purchaseService.delete(id);
        return ApiResponse.ok("Purchase deleted", null);
    }
}
