package com.resturant.management.rms.dining.dto;

import com.resturant.management.rms.dining.TableStatus;
import com.resturant.management.rms.dining.TableZone;
import jakarta.validation.constraints.*;

public final class DiningDtos {

    private DiningDtos() {
    }

    public record TableRequest(
            @NotBlank(message = "{valid.required}") @Size(max = 50) String name,
            @NotNull(message = "{valid.required}")
            @Positive(message = "{valid.min1}")
            @Max(value = 50, message = "{valid.tooLarge}") Integer seats,
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
            @NotNull(message = "{valid.required}") TableStatus status
    ) {}

    /** Counts behind the cashier's table picker legend. */
    public record TableSummary(long free, long occupied, long reserved, long total) {}
}
