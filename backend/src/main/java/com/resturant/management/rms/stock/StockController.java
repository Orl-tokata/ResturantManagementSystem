package com.resturant.management.rms.stock;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import com.resturant.management.rms.stock.dto.StockDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Stock", description = "Ingredients and consumables (ADMIN only)")
public class StockController {

    private final StockService stockService;

    @GetMapping
    @Operation(summary = "List stock items (paged)")
    public ApiResponse<PageResponse<StockItemResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("name").ascending());
        return ApiResponse.ok(PageResponse.from(stockService.search(search, pageable)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Stock totals", description = "Item count, valuation, low and out-of-stock counts.")
    public ApiResponse<StockSummary> summary() {
        return ApiResponse.ok(stockService.summary());
    }

    @GetMapping("/low")
    @Operation(summary = "Items below their reorder level")
    public ApiResponse<List<StockItemResponse>> lowStock() {
        return ApiResponse.ok(stockService.lowStock());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one stock item")
    public ApiResponse<StockItemResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(stockService.get(id));
    }

    @GetMapping("/{id}/movements")
    @Operation(summary = "Movement history for an item",
            description = "The audit trail: every quantity change leaves a row here.")
    public ApiResponse<PageResponse<MovementResponse>> movements(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100));
        return ApiResponse.ok(PageResponse.from(stockService.movements(id, pageable)));
    }

    @PostMapping
    @Operation(summary = "Create a stock item")
    public ResponseEntity<ApiResponse<StockItemResponse>> create(
            @Valid @RequestBody StockItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(stockService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a stock item",
            description = "Editing the quantity directly leaves no audit trail — prefer /adjust.")
    public ApiResponse<StockItemResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody StockItemRequest request) {
        return ApiResponse.ok("Stock item updated", stockService.update(id, request));
    }

    @PostMapping("/{id}/adjust")
    @Operation(summary = "Adjust a quantity",
            description = "IN adds, OUT and DAMAGED remove. Always send a positive qty — the "
                        + "direction comes from the type. Writes a movement row.")
    public ApiResponse<StockItemResponse> adjust(@PathVariable Long id,
                                                 @Valid @RequestBody AdjustRequest request,
                                                 @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok("Stock adjusted",
                stockService.adjust(id, request, principal.getUsername()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a stock item", description = "400 once it has movement history.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        stockService.delete(id);
        return ApiResponse.ok("Stock item deleted", null);
    }
}
