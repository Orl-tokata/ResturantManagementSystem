package com.resturant.management.rms.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {

    private ReportDtos() {
    }

    /** The admin dashboard, in one request. */
    public record DashboardSummary(
            BigDecimal todaySales,
            BigDecimal monthSales,
            long paidInvoices,
            long lowStockCount,
            long tablesFree,
            long tablesOccupied,
            long tablesReserved,
            List<DailyPoint> lastThirtyDays,
            List<BestSeller> bestSellers,
            List<LowStockRow> lowStock
    ) {}

    /** The cashier home screen. */
    public record CashierSummary(
            BigDecimal todaySales,
            long todayInvoices,
            long tablesOccupied,
            long tablesTotal,
            long openBills
    ) {}

    public record DailyPoint(LocalDate date, BigDecimal total, long orders) {}

    /**
     * Takings in one hour of the day, summed across the whole range.
     *
     * @param hour 0–23, local time, the hour the payment was taken
     */
    public record HourlyPoint(int hour, BigDecimal total, long orders) {}

    public record BestSeller(String productName, BigDecimal qty, BigDecimal revenue) {}

    public record LowStockRow(Long id, String name, String unit, BigDecimal qty, BigDecimal minQty) {}

    public record CategoryRevenue(
            String name,
            String nameEn,
            BigDecimal qty,
            BigDecimal revenue,
            BigDecimal percent
    ) {}

    /** Header figures plus every series the reports screen draws. */
    public record SalesReport(
            LocalDate from,
            LocalDate to,
            BigDecimal revenue,
            BigDecimal cost,
            BigDecimal grossProfit,
            BigDecimal marginPercent,
            long invoiceCount,
            BigDecimal averageSale,
            List<DailyPoint> daily,
            List<HourlyPoint> hourly,
            List<CategoryRevenue> byCategory,
            List<BestSeller> bestSellers,
            /**
             * How many sold lines in this range carry a cost that was filled
             * in by V13 rather than recorded at the time. Any number above
             * zero means the margin here is an estimate, and the screen says
             * so — a plausible wrong number presented as measured is worse
             * than a labelled gap.
             */
            long estimatedCostLines
    ) {}

    /** One row of the sales detail table, and one line of the CSV export. */
    public record SalesRow(
            LocalDate date,
            String invoiceNo,
            String tableName,
            String cashierName,
            int itemCount,
            BigDecimal total,
            BigDecimal cost,
            BigDecimal profit,
            /** True when any line on this bill never recorded its own cost. */
            boolean costEstimated,
            String paymentMethod
    ) {}
}
