package com.resturant.management.rms.staff;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import com.resturant.management.rms.staff.dto.StaffDtos.StaffRequest;
import com.resturant.management.rms.staff.dto.StaffDtos.StaffResponse;
import com.resturant.management.rms.user.Role;
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

/** Staff records are personnel data, so the whole resource is ADMIN-only. */
@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Staff", description = "Employee records (ADMIN only)")
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    @Operation(summary = "List staff (paged)")
    public ApiResponse<PageResponse<StaffResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("staffCode").ascending());
        return ApiResponse.ok(PageResponse.from(staffService.search(search, role, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one staff member")
    public ApiResponse<StaffResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(staffService.get(id));
    }

    @PostMapping
    @Operation(summary = "Create a staff member")
    public ResponseEntity<ApiResponse<StaffResponse>> create(@Valid @RequestBody StaffRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(staffService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a staff member")
    public ApiResponse<StaffResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody StaffRequest request) {
        return ApiResponse.ok("Staff updated", staffService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a staff member")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        staffService.delete(id);
        return ApiResponse.ok("Staff deleted", null);
    }
}
