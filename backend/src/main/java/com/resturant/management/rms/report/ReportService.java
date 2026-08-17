package com.resturant.management.rms.report;

import com.resturant.management.rms.dining.DiningTableRepository;
import com.resturant.management.rms.dining.TableStatus;
import com.resturant.management.rms.order.Order;
import com.resturant.management.rms.order.OrderItemRepository;
import com.resturant.management.rms.order.OrderRepository;
import com.resturant.management.rms.order.OrderStatus;
import com.resturant.management.rms.report.dto.ReportDtos.*;
import com.resturant.management.rms.stock.StockItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int SCALE = 2;

    private final OrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final DiningTableRepository tableRepository;
    private final StockItemRepository stockRepository;

    /* ===================================================================== */
    /* Dashboards                                                            */
    /* ===================================================================== */

    @Transactional(readOnly = true)
    public DashboardSummary dashboard() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        return new DashboardSummary(
                revenue(today, today),
                revenue(monthStart, today),
                orderRepository.countAllPaid(),
                stockRepository.countLowStock(),
                tableRepository.countByStatus(TableStatus.FREE),
                tableRepository.countByStatus(TableStatus.OCCUPIED),
                tableRepository.countByStatus(TableStatus.RESERVED),
                dailySeries(today.minusDays(6), today),
                bestSellers(monthStart, today, 5),
                stockRepository.findLowStock().stream()
                        .limit(5)
                        .map(s -> new LowStockRow(s.getId(), s.getName(), s.getUnit(),
                                s.getQty(), s.getMinQty()))
                        .toList());
    }

    @Transactional(readOnly = true)
    public CashierSummary cashierHome() {
        LocalDate today = LocalDate.now();
        return new CashierSummary(
                revenue(today, today),
                orderRepository.countByStatusAndPaidAtBetween(
                        OrderStatus.PAID, startOf(today), endOf(today)),
                tableRepository.countByStatus(TableStatus.OCCUPIED),
                tableRepository.count(),
                orderRepository.countByStatusAndRegDtmBetween(
                        OrderStatus.OPEN, startOf(today.minusDays(30)), endOf(today)));
    }

    /* ===================================================================== */
    /* Sales report                                                          */
    /* ===================================================================== */

    @Transactional(readOnly = true)
    public SalesReport salesReport(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();

        BigDecimal revenue = revenue(start, end);
        BigDecimal cost = scale(itemRepository.sumCostBetween(startOf(start), endOf(end)));
        BigDecimal profit = scale(revenue.subtract(cost));

        BigDecimal margin = revenue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profit.multiply(BigDecimal.valueOf(100))
                        .divide(revenue, SCALE, RoundingMode.HALF_UP);

        long invoices = orderRepository.countByStatusAndPaidAtBetween(
                OrderStatus.PAID, startOf(start), endOf(end));
        BigDecimal average = invoices == 0
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(invoices), SCALE, RoundingMode.HALF_UP);

        return new SalesReport(start, end, revenue, cost, profit, margin, invoices, average,
                dailySeries(start, end), categoryBreakdown(start, end),
                bestSellers(start, end, 10));
    }

    @Transactional(readOnly = true)
    public List<SalesRow> salesRows(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();

        List<Order> orders = orderRepository.findByStatusAndPaidAtBetweenOrderByPaidAtDesc(
                OrderStatus.PAID, startOf(start), endOf(end));
        if (orders.isEmpty()) return List.of();

        // One extra query for all the costs, rather than one per row.
        Map<Long, BigDecimal> costs = itemRepository
                .costByOrder(orders.stream().map(Order::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> scale((BigDecimal) row[1])));

        return orders.stream().map(o -> {
            BigDecimal cost = costs.getOrDefault(o.getId(), BigDecimal.ZERO);
            return new SalesRow(
                    o.getPaidAt() != null ? o.getPaidAt().toLocalDate() : null,
                    o.getInvoiceNo(),
                    o.getTable() != null ? o.getTable().getName() : "",
                    o.getCashier() != null ? o.getCashier().getUserNm() : "",
                    o.getItems().size(),
                    o.getTotal(),
                    cost,
                    scale(o.getTotal().subtract(cost)),
                    o.getPaymentMethod() != null ? o.getPaymentMethod().name() : "");
        }).toList();
    }

    /** RFC 4180-ish CSV of the sales detail. */
    @Transactional(readOnly = true)
    public String salesCsv(LocalDate from, LocalDate to) {
        StringBuilder csv = new StringBuilder();
        // BOM so Excel opens the Khmer names in the right encoding.
        csv.append('﻿');
        csv.append("Date,Invoice,Table,Cashier,Items,Total,Cost,Profit,Payment\n");

        for (SalesRow r : salesRows(from, to)) {
            csv.append(join(
                    String.valueOf(r.date()), r.invoiceNo(), r.tableName(), r.cashierName(),
                    String.valueOf(r.itemCount()), String.valueOf(r.total()),
                    String.valueOf(r.cost()), String.valueOf(r.profit()), r.paymentMethod()));
            csv.append('\n');
        }
        return csv.toString();
    }

    private String join(String... cells) {
        return Arrays.stream(cells).map(this::escape).collect(Collectors.joining(","));
    }

    /** Quote any cell containing a comma, quote or newline, doubling inner quotes. */
    private String escape(String value) {
        String v = value == null ? "" : value;
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    /* ===================================================================== */
    /* Building blocks                                                       */
    /* ===================================================================== */

    private BigDecimal revenue(LocalDate from, LocalDate to) {
        return scale(orderRepository.sumRevenueBetween(startOf(from), endOf(to)));
    }

    /**
     * Daily totals with **every** day in the range present, zero-filled.
     *
     * <p>A chart that skips quiet days misleads: gaps get drawn as if the days
     * did not exist, so a slow Tuesday looks like it never happened.
     */
    private List<DailyPoint> dailySeries(LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> totals = new HashMap<>();
        Map<LocalDate, Long> counts = new HashMap<>();

        for (Object[] row : orderRepository.findPaidTotals(startOf(from), endOf(to))) {
            LocalDate day = ((LocalDateTime) row[0]).toLocalDate();
            BigDecimal total = (BigDecimal) row[1];
            totals.merge(day, total, BigDecimal::add);
            counts.merge(day, 1L, Long::sum);
        }

        List<DailyPoint> series = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            series.add(new DailyPoint(d,
                    scale(totals.getOrDefault(d, BigDecimal.ZERO)),
                    counts.getOrDefault(d, 0L)));
        }
        return series;
    }

    /**
     * Revenue split by category, each with its share of the breakdown.
     *
     * <p>The percentage divides by the sum of the categories, not by headline
     * revenue. Headline revenue is VAT-inclusive while line totals are not, so
     * dividing by it produced shares that summed to ~91% — an apples-to-oranges
     * ratio that reads as missing money. These shares sum to 100%.
     */
    private List<CategoryRevenue> categoryBreakdown(LocalDate from, LocalDate to) {
        List<Object[]> rows = itemRepository.revenueByCategory(startOf(from), endOf(to));

        BigDecimal basis = rows.stream()
                .map(row -> (BigDecimal) row[3])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream()
                .map(row -> {
                    BigDecimal amount = scale((BigDecimal) row[3]);
                    BigDecimal percent = basis.compareTo(BigDecimal.ZERO) == 0
                            ? BigDecimal.ZERO
                            : amount.multiply(BigDecimal.valueOf(100))
                                    .divide(basis, 1, RoundingMode.HALF_UP);
                    return new CategoryRevenue(
                            (String) row[0], (String) row[1], (BigDecimal) row[2], amount, percent);
                })
                .toList();
    }

    private List<BestSeller> bestSellers(LocalDate from, LocalDate to, int limit) {
        return itemRepository.findBestSellers(startOf(from), endOf(to)).stream()
                .limit(limit)
                .map(row -> new BestSeller(
                        (String) row[0], (BigDecimal) row[1], scale((BigDecimal) row[2])))
                .toList();
    }

    private static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static LocalDateTime startOf(LocalDate date) {
        return date.atStartOfDay();
    }

    private static LocalDateTime endOf(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }
}
