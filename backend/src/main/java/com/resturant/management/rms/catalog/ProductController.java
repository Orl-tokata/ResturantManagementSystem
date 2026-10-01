package com.resturant.management.rms.catalog;

import com.resturant.management.rms.common.Paging;
import com.resturant.management.rms.catalog.dto.CatalogDtos.ProductRequest;
import com.resturant.management.rms.catalog.dto.CatalogDtos.ProductResponse;
import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.resturant.management.rms.storage.ImageStorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;

import java.time.Duration;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Products", description = "Menu items")
public class ProductController {

    private final ProductService productService;
    private final ImageStorageService imageStorage;

    @GetMapping
    @Operation(summary = "List products (paged)",
            description = "Optional free-text search and category filter — also backs the POS grid.")
    public ApiResponse<PageResponse<ProductResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = Paging.of(page, size, Sort.by("name").ascending());
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

    /* ---- Photographs ----------------------------------------------------- */

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload a product photograph",
            description = "JPEG or PNG. The file is decoded and written out again as a "
                    + "scaled JPEG, so nothing the caller sent is stored as-is. Replaces "
                    + "any previous photograph.")
    public ApiResponse<ProductResponse> uploadImage(@PathVariable Long id,
                                                    @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(productService.setImage(id, file));
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove a product photograph",
            description = "The product keeps its icon, which is what the screens then show.")
    public ApiResponse<ProductResponse> deleteImage(@PathVariable Long id) {
        return ApiResponse.ok(productService.clearImage(id));
    }

    /**
     * Serves a stored photograph.
     *
     * <p>Public, and deliberately so. An {@code <img src>} carries no
     * Authorization header — the axios interceptor that adds one only applies
     * to requests axios makes — so an authenticated URL here would mean every
     * tile in the POS fetching bytes by hand and holding blob URLs. These are
     * pictures of food on a menu; the trade is not close.
     *
     * <p>Returns no envelope: it is an image, and a browser is the client.
     */
    @GetMapping("/images/{name}")
    @Operation(summary = "Fetch a product photograph")
    public ResponseEntity<byte[]> image(@PathVariable String name) {
        byte[] bytes = imageStorage.load(name);
        if (bytes == null) {
            // 404 rather than an error envelope: the caller is an <img>, and
            // the screens already fall back to the icon when one does not load.
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                // The name contains a UUID and the content never changes under
                // it, so this can be cached hard. A replacement gets a new name.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .contentType(MediaType.parseMediaType(imageStorage.contentType(name)))
                .body(bytes);
    }
}
