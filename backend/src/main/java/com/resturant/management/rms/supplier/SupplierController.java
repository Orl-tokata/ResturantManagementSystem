package com.resturant.management.rms.supplier;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import com.resturant.management.rms.supplier.dto.SupplierDtos.SupplierRequest;
import com.resturant.management.rms.supplier.dto.SupplierDtos.SupplierResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Supplier records include payables, so the whole resource is ADMIN-only. */
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Suppliers", description = "Supplier records and payables (ADMIN only)")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @Operation(summary = "List suppliers (paged)")
    public ApiResponse<PageResponse<SupplierResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("supplierCode").ascending());
        return ApiResponse.ok(PageResponse.from(supplierService.search(search, pageable)));
    }

    @GetMapping("/active")
    @Operation(summary = "Active suppliers, un-paged",
            description = "Feeds the supplier dropdown when raising a purchase order.")
    public ApiResponse<List<SupplierResponse>> listActive() {
        return ApiResponse.ok(supplierService.listActive());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one supplier")
    public ApiResponse<SupplierResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(supplierService.get(id));
    }

    @PostMapping
    @Operation(summary = "Create a supplier")
    public ResponseEntity<ApiResponse<SupplierResponse>> create(
            @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(supplierService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a supplier",
            description = "The balance is not settable here — it moves only when goods are received.")
    public ApiResponse<SupplierResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody SupplierRequest request) {
        return ApiResponse.ok("Supplier updated", supplierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a supplier",
            description = "409 if any purchase order references it; 400 if a balance is outstanding.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ApiResponse.ok("Supplier deleted", null);
    }
}
