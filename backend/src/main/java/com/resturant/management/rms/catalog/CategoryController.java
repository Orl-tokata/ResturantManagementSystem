package com.resturant.management.rms.catalog;

import com.resturant.management.rms.catalog.dto.CatalogDtos.CategoryRequest;
import com.resturant.management.rms.catalog.dto.CatalogDtos.CategoryResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Categories", description = "Menu categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List categories (paged)")
    public ApiResponse<PageResponse<CategoryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("sortOrder").ascending());
        return ApiResponse.ok(PageResponse.from(categoryService.search(search, pageable)));
    }

    @GetMapping("/active")
    @Operation(summary = "All active categories, un-paged",
            description = "Feeds the POS category rail and the category dropdowns.")
    public ApiResponse<List<CategoryResponse>> listActive() {
        return ApiResponse.ok(categoryService.listActive());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one category")
    public ApiResponse<CategoryResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a category (ADMIN)")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a category (ADMIN)")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok("Category updated", categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category (ADMIN)",
            description = "409 if any product still references it.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok("Category deleted", null);
    }
}
