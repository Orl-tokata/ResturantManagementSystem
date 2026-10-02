package com.resturant.management.rms.setting;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.LocalizedArg;
import com.resturant.management.rms.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Settings", description = "Application settings")
public class SettingController {

    /**
     * Keys the POS reads on every sale. A bad value here would corrupt every
     * subsequent bill, so these are validated on the way in rather than being
     * left to the fallback in {@link SettingService}.
     */
    private static final Map<String, String> NUMERIC_KEYS = Map.of(
            SettingService.VAT_RATE, "setting.vatRate",
            SettingService.KHR_RATE, "setting.khrRate");

    private final SettingService settingService;
    private final FxRateService fxRates;

    @GetMapping
    @Operation(summary = "All settings as a key/value map",
            description = "Readable by any authenticated user — the POS needs the VAT and "
                        + "riel rate to show live totals.")
    public ApiResponse<Map<String, String>> getAll() {
        return ApiResponse.ok(settingService.getAll());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update settings (ADMIN)",
            description = "Partial update: only the keys sent are written, the rest are left alone.")
    public ApiResponse<Map<String, String>> update(
            @NotEmpty(message = "{valid.atLeastOneSetting}")
            @RequestBody Map<String, String> values,
            @AuthenticationPrincipal UserDetails principal) {

        validate(values);
        settingService.putAll(values);

        // Changing the rate here is the only way it ever changes, so this is
        // the only place its history can be kept. validate() has already
        // refused anything that is not a positive number.
        String rate = values.get(SettingService.KHR_RATE);
        if (rate != null) {
            fxRates.record(FxRateService.KHR, new BigDecimal(rate.trim()),
                    principal != null ? principal.getUsername() : null);
        }
        return ApiResponse.ok("Settings saved", settingService.getAll());
    }

    @GetMapping("/fx-rates")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "What the riel has been worth (ADMIN)",
            description = "Newest first. One row per day the rate changed; a receipt reads "
                        + "the rate stamped on its own order, not this.")
    public ApiResponse<List<FxRateRow>> fxRates() {
        return ApiResponse.ok(fxRates.history(FxRateService.KHR, 24).stream()
                .map(r -> new FxRateRow(r.getRate(), r.getValidFrom(), r.getCreatedBy()))
                .toList());
    }

    public record FxRateRow(BigDecimal rate, LocalDate validFrom, String changedBy) {}

    private void validate(Map<String, String> values) {
        NUMERIC_KEYS.forEach((key, labelKey) -> {
            String raw = values.get(key);
            if (raw == null) return;
            try {
                BigDecimal value = new BigDecimal(raw.trim());
                if (value.compareTo(BigDecimal.ZERO) < 0) {
                    throw new BadRequestException("error.setting.negative", new LocalizedArg(labelKey));
                }
                if (key.equals(SettingService.VAT_RATE) && value.compareTo(BigDecimal.valueOf(100)) > 0) {
                    throw new BadRequestException("error.setting.vatTooHigh");
                }
                if (key.equals(SettingService.KHR_RATE) && value.compareTo(BigDecimal.ZERO) == 0) {
                    throw new BadRequestException("error.setting.rateZero");
                }
            } catch (NumberFormatException e) {
                throw new BadRequestException("error.setting.notNumber", new LocalizedArg(labelKey), raw);
            }
        });
    }
}
