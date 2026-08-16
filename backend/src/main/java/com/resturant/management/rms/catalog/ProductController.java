package com.resturant.management.rms.catalog;

import com.resturant.management.rms.catalog.dto.CatalogDtos.ProductRequest;
import com.resturant.management.rms.catalog.dto.CatalogDtos.ProductResponse;
import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
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

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Products", description = "Menu items")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List products (paged)",
            description = "Optional free-text search and category filter — also backs the POS grid.")
    public ApiResponse<PageResponse<ProductResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 200), Sort.by("name").ascending());
        return ApiResponse.ok(PageResponse.from(productService.search(search, categoryId, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one product")
    public ApiResponse<ProductResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(productService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a product (ADMIN)")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(productService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a product (ADMIN)")
    public ApiResponse<ProductResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok("Product updated", productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a product (ADMIN)")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ApiResponse.ok("Product deleted", null);
    }
}
