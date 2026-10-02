package com.resturant.management.rms.shift.dto;

import com.resturant.management.rms.shift.CashMovementType;
import com.resturant.management.rms.shift.ShiftStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ShiftDtos {

    private ShiftDtos() {
    }

    /** Opening is one number: what was counted into the drawer. */
    public record OpenShiftRequest(
            @NotNull(message = "{valid.required}")
            @PositiveOrZero(message = "{valid.notNegative}")
            BigDecimal openingFloat,
            @Size(max = 500) String note
    ) {}

    /**
     * Closing is one number too — what is in the drawer now. The expected
     * figure is not sent: a till that accepted it would be letting the screen
     * decide whether it balances.
     */
    public record CloseShiftRequest(
            @NotNull(message = "{valid.required}")
            @PositiveOrZero(message = "{valid.notNegative}")
            BigDecimal declaredCash,
            @Size(max = 500) String note
    ) {}

    public record CashMovementRequest(
            @NotNull(message = "{valid.required}") CashMovementType type,
            @NotNull(message = "{valid.required}")
            @Positive(message = "{valid.positive}")
            BigDecimal amount,
            @NotBlank(message = "{valid.required}")
            @Size(max = 500) String reason
    ) {}

    public record CashMovementResponse(
            Long id,
            CashMovementType type,
            BigDecimal amount,
            /** True when this put money in. The amount itself is always positive. */
            boolean increase,
            String reason,
            String refType,
            Long refId,
            String createdBy,
            LocalDateTime createdAt
    ) {}

    /** One line of the Z-report's breakdown. */
    public record MovementTotal(
            CashMovementType type,
            BigDecimal total,
            long count
    ) {}

    public record ShiftResponse(
            Long id,
            Long userId,
            String userName,
            ShiftStatus status,
            LocalDateTime openedAt,
            LocalDateTime closedAt,
            BigDecimal openingFloat,
            /**
             * What should be in the drawer. Derived from the movements while
             * the shift is open, and the figure recorded at close afterwards —
             * so a correction made later cannot change what the count was
             * measured against.
             */
            BigDecimal expectedCash,
            BigDecimal declaredCash,
            BigDecimal variance,
            String note,
            List<MovementTotal> totals,
            BigDecimal cashSales,
            long saleCount
    ) {}

    /** The shift, its arithmetic, and every movement behind it. */
    public record ShiftDetail(
            ShiftResponse shift,
            List<CashMovementResponse> movements
    ) {}
}
