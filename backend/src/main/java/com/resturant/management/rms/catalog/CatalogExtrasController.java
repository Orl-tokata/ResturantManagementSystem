package com.resturant.management.rms.catalog;

import com.resturant.management.rms.catalog.dto.CatalogDtos.*;
import com.resturant.management.rms.common.ApiResponse;
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

/**
 * API §6.5: sizes on a product, and the questions a product asks.
 *
 * <p>Variants sit under their product's path rather than having one of their
 * own, which is SCREENS §4's point stated as a URL: a separate variant manager
 * means two places to look for one product's price.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Catalog extras", description = "Variants and modifiers")
public class CatalogExtrasController {

    private final CatalogExtrasService service;

    /* ---- Variants ---------------------------------------------------------- */

    @GetMapping("/products/{id}/variants")
    @Operation(summary = "A product's sizes", description = "Empty for most of the menu.")
    public ApiResponse<List<VariantResponse>> variants(@PathVariable Long id) {
        return ApiResponse.ok(service.variantsOf(id));
    }

    @PostMapping("/products/{id}/variants")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Add a size")
    public ResponseEntity<ApiResponse<VariantResponse>> addVariant(
            @PathVariable Long id, @Valid @RequestBody VariantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(service.addVariant(id, request)));
    }

    @PutMapping("/products/{id}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a size")
    public ApiResponse<VariantResponse> updateVariant(
            @PathVariable Long id, @PathVariable Long variantId,
            @Valid @RequestBody VariantRequest request) {
        return ApiResponse.ok("Variant updated", service.updateVariant(id, variantId, request));
    }

    @DeleteMapping("/products/{id}/variants/{variantId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Remove a size",
            description = "Bills that sold it keep the name they sold it under, so history "
                        + "survives this.")
    public ApiResponse<Void> deleteVariant(@PathVariable Long id, @PathVariable Long variantId) {
        service.deleteVariant(id, variantId);
        return ApiResponse.ok("Variant deleted", null);
    }

    /* ---- Modifier groups --------------------------------------------------- */

    @GetMapping("/modifier-groups")
    @Operation(summary = "Every question the menu can ask",
            description = "Shared across products: 'sugar level' is one question however "
                        + "many drinks ask it.")
    public ApiResponse<List<ModifierGroupResponse>> groups() {
        return ApiResponse.ok(service.allGroups());
    }

    @PostMapping("/modifier-groups")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a question and its answers")
    public ResponseEntity<ApiResponse<ModifierGroupResponse>> createGroup(
            @Valid @RequestBody ModifierGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(service.createGroup(request)));
    }

    @PutMapping("/modifier-groups/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Replace a question and its answers")
    public ApiResponse<ModifierGroupResponse> updateGroup(
            @PathVariable Long id, @Valid @RequestBody ModifierGroupRequest request) {
        return ApiResponse.ok("Modifier group updated", service.updateGroup(id, request));
    }

    @DeleteMapping("/modifier-groups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a question")
    public ApiResponse<Void> deleteGroup(@PathVariable Long id) {
        service.deleteGroup(id);
        return ApiResponse.ok("Modifier group deleted", null);
    }

    /* ---- Attaching --------------------------------------------------------- */

    @GetMapping("/products/{id}/modifier-groups")
    @Operation(summary = "The questions this product asks")
    public ApiResponse<List<ModifierGroupResponse>> groupsOf(@PathVariable Long id) {
        return ApiResponse.ok(service.groupsOf(id));
    }

    @PostMapping("/products/{id}/modifier-groups/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Ask this question about this product")
    public ApiResponse<List<ModifierGroupResponse>> attach(
            @PathVariable Long id, @PathVariable Long groupId) {
        return ApiResponse.ok("Attached", service.attach(id, groupId));
    }

    @DeleteMapping("/products/{id}/modifier-groups/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Stop asking it")
    public ApiResponse<List<ModifierGroupResponse>> detach(
            @PathVariable Long id, @PathVariable Long groupId) {
        return ApiResponse.ok("Detached", service.detach(id, groupId));
    }
}
