package com.resturant.management.rms.promotion.dto;

import com.resturant.management.rms.promotion.PromotionScope;
import com.resturant.management.rms.promotion.PromotionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class PromotionDtos {

    private PromotionDtos() {
    }

    public record PromotionRequest(
            @NotBlank(message = "{valid.required}") @Size(max = 150) String name,
            @NotNull(message = "{valid.required}") PromotionType type,
            /** A percentage for PERCENT, money for AMOUNT. */
            @NotNull(message = "{valid.required}")
            @Positive(message = "{valid.positive}")
            BigDecimal value,
            @NotNull(message = "{valid.required}") PromotionScope scope,
            /** The product or category; must be absent for a whole-bill rule. */
            Long scopeId,
            @PositiveOrZero(message = "{valid.notNegative}") BigDecimal minAmount,
            @NotNull(message = "{valid.required}") LocalDateTime startsAt,
            @NotNull(message = "{valid.required}") LocalDateTime endsAt,
            /** Happy hour. Both absent means all day. */
            LocalTime timeFrom,
            LocalTime timeTo,
            Boolean active
    ) {}

    public record PromotionResponse(
            Long id,
            String name,
            PromotionType type,
            BigDecimal value,
            PromotionScope scope,
            Long scopeId,
            /** What the scope names, so a list does not read as a column of ids. */
            String scopeName,
            BigDecimal minAmount,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            LocalTime timeFrom,
            LocalTime timeTo,
            boolean active,
            /** Whether it would fire right now — dates, switch and clock together. */
            boolean liveNow
    ) {}

    /** One rule that actually came off a bill, and how much. */
    public record AppliedPromotion(
            Long promotionId,
            String name,
            PromotionScope scope,
            /** The dish it applied to, or null for a whole-bill rule. */
            String productName,
            BigDecimal discount
    ) {}
}
