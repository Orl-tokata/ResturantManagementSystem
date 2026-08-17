package com.resturant.management.rms.report;

import com.resturant.management.rms.common.ApiResponse;
import com.resturant.management.rms.report.dto.ReportDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reports", description = "Dashboards, sales analysis and CSV export")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin dashboard",
            description = "KPI tiles, a zero-filled 7-day series, best sellers and low stock.")
    public ApiResponse<DashboardSummary> dashboard() {
        return ApiResponse.ok(reportService.dashboard());
    }

    @GetMapping("/dashboard/cashier")
    @Operation(summary = "Cashier home",
            description = "Any authenticated user — a cashier sees their own shift's figures.")
    public ApiResponse<CashierSummary> cashierHome() {
        return ApiResponse.ok(reportService.cashierHome());
    }

    @GetMapping("/reports/sales")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sales report",
            description = "Revenue, cost, gross profit and margin, plus daily, category and "
                        + "best-seller series. Defaults to the current month.")
    public ApiResponse<SalesReport> sales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.salesReport(from, to));
    }

    @GetMapping("/reports/sales/detail")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sales detail rows", description = "One row per paid invoice.")
    public ApiResponse<List<SalesRow>> salesDetail(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(reportService.salesRows(from, to));
    }

    /**
     * Returns raw CSV rather than the {@link ApiResponse} envelope — the browser
     * saves the body to a file, so it must be the file, not JSON around it.
     */
    @GetMapping(value = "/reports/sales.csv", produces = "text/csv")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export sales detail as CSV")
    public ResponseEntity<byte[]> salesCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        byte[] body = reportService.salesCsv(from, to).getBytes(StandardCharsets.UTF_8);
        String filename = "sales-%s-to-%s.csv".formatted(
                from != null ? from : LocalDate.now().withDayOfMonth(1),
                to != null ? to : LocalDate.now());

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
