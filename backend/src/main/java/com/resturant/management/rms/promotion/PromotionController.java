package com.resturant.management.rms.promotion;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import com.resturant.management.rms.common.Paging;
import com.resturant.management.rms.promotion.dto.PromotionDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Promotions", description = "Rules that take money off by themselves")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Promotions (paged)", description = "Newest window first.")
    public ApiResponse<PageResponse<PromotionResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.from(promotionService.list(Paging.of(page, size))));
    }

    @GetMapping("/live")
    @Operation(summary = "Rules running at this moment",
            description = "Dates, switch and clock together. The till shows these so a "
                        + "cashier can answer \"is the offer on?\".")
    public ApiResponse<List<PromotionResponse>> live() {
        return ApiResponse.ok(promotionService.live());
    }

    /**
     * API §6.6: computed server-side, always. A client that decides its own
     * discount decides its own price. This reads what the recalculation
     * already applied, so the screen and the bill cannot disagree.
     */
    @GetMapping("/applicable")
    @Operation(summary = "What came off this bill",
            description = "The rules that actually applied, with the amount each took.")
    public ApiResponse<List<AppliedPromotion>> applicable(@RequestParam Long orderId) {
        return ApiResponse.ok(promotionService.applicableTo(orderId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "One promotion")
    public ApiResponse<PromotionResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(promotionService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a promotion",
            description = "Percent and fixed amount only — SCREENS §3.5 holds buy-X-get-Y "
                        + "back because it interacts with returns, loyalty and tax.")
    public ResponseEntity<ApiResponse<PromotionResponse>> create(
            @Valid @RequestBody PromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(promotionService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a promotion")
    public ApiResponse<PromotionResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody PromotionRequest request) {
        return ApiResponse.ok("Promotion updated", promotionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a promotion",
            description = "400 once a bill has used it — switch it off instead, which is "
                        + "what ending a promotion means.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        promotionService.delete(id);
        return ApiResponse.ok("Promotion deleted", null);
    }
}
