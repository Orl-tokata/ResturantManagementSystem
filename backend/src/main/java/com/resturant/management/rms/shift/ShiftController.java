package com.resturant.management.rms.shift;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import com.resturant.management.rms.common.Paging;
import com.resturant.management.rms.shift.dto.ShiftDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Shifts", description = "Opening, counting and closing a till")
public class ShiftController {

    private final ShiftService shiftService;

    @GetMapping("/current")
    @Operation(summary = "The signed-in user's open shift",
            description = "`data` is null when there is none, which is what the POS gate reads. "
                        + "Not a 404: having no shift open is an ordinary state, not a mistake.")
    public ApiResponse<ShiftResponse> current(@AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok(shiftService.current(principal.getUsername()).orElse(null));
    }

    @PostMapping
    @Operation(summary = "Open a shift",
            description = "One number: the cash counted into the drawer. Refused when this "
                        + "cashier already has one open — the database enforces that, not "
                        + "just this check.")
    public ResponseEntity<ApiResponse<ShiftResponse>> open(
            @Valid @RequestBody OpenShiftRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        ShiftResponse shift = shiftService.open(request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(shift));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a shift against a counted drawer",
            description = "Send what is in the till; the expected figure is worked out here. "
                        + "A non-zero variance needs a note.")
    public ApiResponse<ShiftResponse> close(@PathVariable Long id,
                                            @Valid @RequestBody CloseShiftRequest request,
                                            @AuthenticationPrincipal UserDetails principal) {
        return ApiResponse.ok("Shift closed", shiftService.close(id, request, principal.getUsername()));
    }

    @PostMapping("/{id}/movements")
    @Operation(summary = "Record cash in or out by hand",
            description = "PAY_IN, PAY_OUT, FLOAT or DROP, always with a reason. SALE and "
                        + "REFUND are refused: they follow from bills.")
    public ResponseEntity<ApiResponse<CashMovementResponse>> record(
            @PathVariable Long id,
            @Valid @RequestBody CashMovementRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        CashMovementResponse movement = shiftService.record(id, request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(movement));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One shift, its arithmetic and every movement behind it",
            description = "The Z-report.")
    public ApiResponse<ShiftDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(shiftService.detail(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Shift history (paged)", description = "Newest first.")
    public ApiResponse<PageResponse<ShiftResponse>> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.from(shiftService.history(Paging.of(page, size))));
    }
}
