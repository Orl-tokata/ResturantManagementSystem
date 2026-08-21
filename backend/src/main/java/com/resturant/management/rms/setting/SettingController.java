package com.resturant.management.rms.setting;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.LocalizedArg;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
            @RequestBody Map<String, String> values) {

        validate(values);
        settingService.putAll(values);
        return ApiResponse.ok("Settings saved", settingService.getAll());
    }

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
