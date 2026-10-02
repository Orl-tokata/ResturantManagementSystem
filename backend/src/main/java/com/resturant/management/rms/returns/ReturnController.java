package com.resturant.management.rms.returns;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import com.resturant.management.rms.common.Paging;
import com.resturant.management.rms.returns.dto.ReturnDtos.*;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Returns", description = "Giving money back against a settled bill")
public class ReturnController {

    private final ReturnService returnService;

    /**
     * API §6.3: this exists because no database constraint can prevent
     * over-returning across two documents. The server does the subtraction and
     * the client is told the answer.
     */
    @GetMapping("/orders/{id}/returnable")
    @Operation(summary = "What is still returnable on a sale",
            description = "Every line with how much was sold, how much has already gone back "
                        + "and what remains. 400 if the bill was never settled.")
    public ApiResponse<ReturnableOrder> returnable(@PathVariable Long id) {
        return ApiResponse.ok(returnService.returnable(id));
    }

    @PostMapping("/returns")
    @Operation(summary = "Give money back",
            description = "One transaction: the return document, a stock movement per line, "
                        + "cash out of the drawer, a reversal against the payment and the "
                        + "customer's points. 409 when more is asked for than remains.")
    public ResponseEntity<ApiResponse<ReturnResponse>> create(
            @Valid @RequestBody CreateReturnRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        ReturnResponse created = returnService.create(request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(created));
    }

    @GetMapping("/returns")
    @Operation(summary = "Returns (paged)", description = "Newest first, optionally for one bill.")
    public ApiResponse<PageResponse<ReturnResponse>> list(
            @RequestParam(required = false) Long orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.from(returnService.list(orderId, Paging.of(page, size))));
    }

    @GetMapping("/returns/{id}")
    @Operation(summary = "One return, with its lines", description = "The printable slip.")
    public ApiResponse<ReturnResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(returnService.get(id));
    }
}
