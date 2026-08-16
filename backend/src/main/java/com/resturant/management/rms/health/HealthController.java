package com.resturant.management.rms.health;

import com.resturant.management.rms.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Liveness and dependency checks")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    @Operation(summary = "Service health", description = "Reports app status and database connectivity.")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "rms-backend");
        body.put("status", "UP");
        body.put("time", LocalDateTime.now());

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            body.put("database", "UP");
        } catch (Exception e) {
            body.put("database", "DOWN");
            body.put("databaseError", e.getMessage());
        }

        return ApiResponse.ok(body);
    }
}
