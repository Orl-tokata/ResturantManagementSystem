package com.resturant.management.rms.customer.dto;

import com.resturant.management.rms.customer.LoyaltyType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CustomerRequest(
            @NotBlank(message = "{valid.required}") @Size(max = 150) String name,
            @Size(max = 30) String phone,
            @Email(message = "{valid.email}") @Size(max = 120) String email,
            @Past(message = "{valid.past}") LocalDate birthDate,
            @Size(max = 500) String note
    ) {}

    /**
     * A customer as a screen sees them.
     *
     * <p>{@code points} is summed from the ledger on every read rather than
     * stored, so it cannot drift from the rows behind it.
     */
    public record CustomerResponse(
            Long id,
            String code,
            String name,
            String phone,
            String email,
            LocalDate birthDate,
            String note,
            BigDecimal points,
            /** Totals over settled bills. Zero and null until they buy something. */
            BigDecimal totalSpent,
            long visitCount,
            LocalDateTime lastVisit
    ) {}

    /** A manager putting the balance right, with a reason. */
    public record LoyaltyAdjustRequest(
            @NotNull(message = "{valid.required}") BigDecimal points,
            @NotBlank(message = "{valid.required}") @Size(max = 255) String note
    ) {}

    public record LoyaltyResponse(
            Long id,
            LoyaltyType type,
            /** Signed: positive adds, negative takes away. */
            BigDecimal points,
            Long orderId,
            String invoiceNo,
            String note,
            String createdBy,
            LocalDateTime createdAt
    ) {}
}
