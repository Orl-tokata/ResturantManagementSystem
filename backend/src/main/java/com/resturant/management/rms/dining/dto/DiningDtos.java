package com.resturant.management.rms.dining.dto;

import com.resturant.management.rms.dining.TableStatus;
import com.resturant.management.rms.dining.TableZone;
import jakarta.validation.constraints.*;

public final class DiningDtos {

    private DiningDtos() {
    }

    public record TableRequest(
            @NotBlank(message = "is required") @Size(max = 50) String name,
            @NotNull(message = "is required")
            @Positive(message = "must be at least 1")
            @Max(value = 50, message = "seems too large") Integer seats,
            TableZone zone,
            TableStatus status
    ) {}

    public record TableResponse(
            Long id,
            String name,
            Integer seats,
            TableZone zone,
            TableStatus status
    ) {}

    /** Body of {@code PATCH /api/tables/{id}/status}. */
    public record TableStatusRequest(
            @NotNull(message = "is required") TableStatus status
    ) {}

    /** Counts behind the cashier's table picker legend. */
    public record TableSummary(long free, long occupied, long reserved, long total) {}
}
