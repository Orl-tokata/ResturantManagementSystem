package com.resturant.management.rms.dining;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import com.resturant.management.rms.dining.dto.DiningDtos.TableRequest;
import com.resturant.management.rms.dining.dto.DiningDtos.TableResponse;
import com.resturant.management.rms.dining.dto.DiningDtos.TableStatusRequest;
import com.resturant.management.rms.dining.dto.DiningDtos.TableSummary;
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
@RequestMapping("/api/tables")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Tables", description = "Dining tables")
public class DiningTableController {

    private final DiningTableService tableService;

    @GetMapping
    @Operation(summary = "List tables (paged)")
    public ApiResponse<PageResponse<TableResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TableZone zone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 200), Sort.by("name").ascending());
        return ApiResponse.ok(PageResponse.from(tableService.search(search, zone, pageable)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Counts by status", description = "Backs the cashier table picker legend.")
    public ApiResponse<TableSummary> summary() {
        return ApiResponse.ok(tableService.summary());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one table")
    public ApiResponse<TableResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(tableService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a table (ADMIN)")
    public ResponseEntity<ApiResponse<TableResponse>> create(@Valid @RequestBody TableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(tableService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a table (ADMIN)")
    public ApiResponse<TableResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody TableRequest request) {
        return ApiResponse.ok("Table updated", tableService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change table status",
            description = "Any authenticated user — cashiers seat and clear tables.")
    public ApiResponse<TableResponse> changeStatus(@PathVariable Long id,
                                                   @Valid @RequestBody TableStatusRequest request) {
        return ApiResponse.ok("Table status updated", tableService.changeStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a table (ADMIN)", description = "409 while the table is occupied.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tableService.delete(id);
        return ApiResponse.ok("Table deleted", null);
    }
}
