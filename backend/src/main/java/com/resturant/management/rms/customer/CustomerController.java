package com.resturant.management.rms.customer;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.PageResponse;
import com.resturant.management.rms.common.Paging;
import com.resturant.management.rms.customer.dto.CustomerDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customers", description = "Customers and their points")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Customers (paged)", description = "Search by name, phone or code.")
    public ApiResponse<PageResponse<CustomerResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = Paging.of(page, size, Sort.by("name").ascending());
        return ApiResponse.ok(PageResponse.from(customerService.search(search, pageable)));
    }

    /**
     * The till's lookup. A list endpoint with paging metadata is the wrong
     * shape for a field a cashier tabs through — API §6.4.
     */
    @GetMapping("/lookup")
    @Operation(summary = "Find by exact phone",
            description = "For the POS. Returns every customer with that number, because the "
                        + "column is not unique — a couple sharing one is ordinary.")
    public ApiResponse<List<CustomerResponse>> lookup(@RequestParam String phone) {
        return ApiResponse.ok(customerService.lookup(phone));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One customer")
    public ApiResponse<CustomerResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(customerService.get(id));
    }

    @PostMapping
    @Operation(summary = "Register a customer",
            description = "The code is generated. A cashier should never have to invent one "
                        + "with somebody waiting.")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(
            @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(customerService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a customer")
    public ApiResponse<CustomerResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok("Customer updated", customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a customer",
            description = "400 once they have bills or points — deleting then would orphan a "
                        + "receipt and silently change a balance.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ApiResponse.ok("Customer deleted", null);
    }

    @GetMapping("/{id}/loyalty")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "The points ledger",
            description = "Newest first. The balance on the customer is this, summed.")
    public ApiResponse<PageResponse<LoyaltyResponse>> ledger(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.from(customerService.ledger(id, Paging.of(page, size))));
    }

    @PostMapping("/{id}/loyalty")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Adjust the balance by hand",
            description = "Signed: positive adds, negative takes away. The note is required — "
                        + "an unexplained correction to someone's points is the one thing "
                        + "this ledger exists to prevent.")
    public ResponseEntity<ApiResponse<LoyaltyResponse>> adjust(
            @PathVariable Long id,
            @Valid @RequestBody LoyaltyAdjustRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        customerService.adjust(id, request, principal.getUsername())));
    }
}
