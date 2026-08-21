package com.resturant.management.rms.order;

import com.resturant.management.rms.catalog.Product;
import com.resturant.management.rms.catalog.ProductRepository;
import com.resturant.management.rms.common.Strings;
import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.dining.DiningTable;
import com.resturant.management.rms.dining.DiningTableRepository;
import com.resturant.management.rms.dining.TableStatus;
import com.resturant.management.rms.order.dto.OrderDtos.*;
import com.resturant.management.rms.setting.SettingService;
import com.resturant.management.rms.user.UserInfm;
import com.resturant.management.rms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int MONEY_SCALE = 2;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final DiningTableRepository tableRepository;
    private final UserRepository userRepository;
    private final SettingService settings;

    /* ===================================================================== */
    /* Opening a bill                                                        */
    /* ===================================================================== */

    /**
     * Returns the bill already open at this table, or starts a new one.
     *
     * <p>Reusing rather than erroring matters in practice: a cashier tapping the
     * same table twice, or two tills touching one table, must not produce two
     * competing bills.
     */
    @Transactional
    public OrderResponse openOrReuse(OpenOrderRequest request, String username) {
        DiningTable table = tableRepository.findById(request.tableId())
                .orElseThrow(() -> NotFoundException.of("entity.table", request.tableId()));

        List<Order> open = orderRepository.findByTableIdAndStatus(table.getId(), OrderStatus.OPEN);
        if (!open.isEmpty()) {
            Order existing = open.get(0);
            if (request.guestCount() != null) {
                existing.setGuestCount(request.guestCount());
                orderRepository.save(existing);
            }
            return toResponse(existing);
        }

        UserInfm cashier = userRepository.findByUserId(username).orElse(null);

        Order order = Order.builder()
                .invoiceNo(nextInvoiceNo())
                .table(table)
                .cashier(cashier)
                .guestCount(request.guestCount())
                .vatRate(settings.vatRate())
                .status(OrderStatus.OPEN)
                .build();

        recalculate(order);
        Order saved = orderRepository.save(order);

        // A bill open at a table means the table is in use.
        table.setStatus(TableStatus.OCCUPIED);
        tableRepository.save(table);

        log.info("Opened {} at {}", saved.getInvoiceNo(), table.getName());
        return toResponse(saved);
    }

    /* ===================================================================== */
    /* Editing the basket                                                    */
    /* ===================================================================== */

    /**
     * Replaces every line on the bill.
     *
     * <p>Unit price is copied from the product at this moment; later price
     * changes must not silently rewrite an open bill.
     */
    @Transactional
    public OrderResponse replaceItems(Long orderId, UpdateItemsRequest request) {
        Order order = findEditable(orderId);

        order.clearItems();

        for (OrderItemRequest line : request.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> NotFoundException.of("entity.product", line.productId()));

            OrderItem item = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .qty(line.qty())
                    .unitPrice(product.getPrice())
                    .note(line.note())
                    .build();
            item.recalculate();
            order.addItem(item);
        }

        if (request.discount() != null) {
            order.setDiscount(request.discount());
        }

        recalculate(order);
        return toResponse(orderRepository.save(order));
    }

    /* ===================================================================== */
    /* Payment                                                               */
    /* ===================================================================== */

    /**
     * Settles a bill. All of it or none of it: mark PAID, record the tender and
     * change, decrement stock for every line, and release the table.
     *
     * <p>Runs in one transaction so a failure part-way cannot leave a bill
     * marked paid with stock untouched, or a table stuck occupied.
     */
    @Transactional
    public OrderResponse pay(Long orderId, PayRequest request) {
        Order order = findEditable(orderId);

        if (order.getItems().isEmpty()) {
            throw new BadRequestException("error.order.emptyBill");
        }

        if (request.discount() != null) {
            order.setDiscount(request.discount());
        }
        recalculate(order);

        BigDecimal total = order.getTotal();
        BigDecimal tendered = request.amountTendered();

        // Cash is the only method where the amount handed over is meaningful;
        // card and QR settle the exact total.
        if (request.paymentMethod() == PaymentMethod.CASH) {
            if (tendered == null) {
                throw new BadRequestException("error.order.tenderedRequired");
            }
            if (tendered.compareTo(total) < 0) {
                throw new BadRequestException("error.order.tenderedShort", tendered, total);
            }
        } else {
            tendered = total;
        }

        order.setPaymentMethod(request.paymentMethod());
        order.setAmountTendered(tendered);
        order.setChangeAmount(tendered.subtract(total).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());

        decrementStock(order);
        freeTable(order);

        Order saved = orderRepository.save(order);
        log.info("Paid {} — {} {} (change {})",
                saved.getInvoiceNo(), saved.getPaymentMethod(), saved.getTotal(), saved.getChangeAmount());
        return toResponse(saved);
    }

    /**
     * Reduces each sold product's stock.
     *
     * <p>Stock is allowed to go negative rather than blocking the sale: the food
     * has already left the kitchen by the time the bill is settled, so refusing
     * payment would be the wrong answer. A negative figure is a signal for the
     * stock screen, not a reason to fail here.
     *
     * <p>Note: no {@code StockMovement} rows are written for sales. Movements are
     * keyed to {@code stock_item} (raw ingredients), and there is no recipe table
     * mapping a dish to its ingredients — see PROJECT-SPEC.md §12.
     */
    private void decrementStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product == null) continue;

            BigDecimal before = product.getStockQty() == null ? BigDecimal.ZERO : product.getStockQty();
            BigDecimal after = before.subtract(item.getQty());
            product.setStockQty(after);
            productRepository.save(product);

            if (after.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Stock for '{}' is now negative ({}) after {}",
                        product.getName(), after, order.getInvoiceNo());
            }
        }
    }

    @Transactional(readOnly = true)
    public ReceiptResponse receipt(Long orderId) {
        return new ReceiptResponse(
                settings.getString("restaurant.name", "ភោជនីយដ្ឋាន"),
                settings.getString("restaurant.nameEn", "Restaurant"),
                settings.getString("restaurant.address", ""),
                settings.getString("restaurant.phone", ""),
                toResponse(find(orderId)));
    }

    @Transactional
    public OrderResponse cancel(Long orderId) {
        Order order = findEditable(orderId);
        order.setStatus(OrderStatus.CANCELLED);
        freeTable(order);
        return toResponse(orderRepository.save(order));
    }

    /* ===================================================================== */
    /* Reads                                                                 */
    /* ===================================================================== */

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return toResponse(find(id));
    }

    /* ---- History --------------------------------------------------------- */

    /**
     * Paged order history.
     *
     * <p>{@code from}/{@code to} are dates from the UI, not instants. They are
     * widened here to cover the whole day — an exclusive {@code to} at midnight
     * would silently drop every bill taken on the last day of the range.
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> history(String query, OrderStatus status,
                                       LocalDate from, LocalDate to, Pageable pageable) {
        return orderRepository
                .search(Strings.blankToNull(query), status, startOf(from), endOf(to), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public HistorySummary historySummary(LocalDate from, LocalDate to) {
        LocalDateTime start = startOf(from);
        LocalDateTime end = endOf(to);

        BigDecimal sales = orderRepository.sumPaidTotalByRegDtmBetween(start, end);
        long paid = orderRepository.countByStatusAndRegDtmBetween(OrderStatus.PAID, start, end);
        long cancelled = orderRepository.countByStatusAndRegDtmBetween(OrderStatus.CANCELLED, start, end);
        long total = orderRepository.countByRegDtmBetween(start, end);

        BigDecimal average = paid == 0
                ? BigDecimal.ZERO
                : sales.divide(BigDecimal.valueOf(paid), MONEY_SCALE, RoundingMode.HALF_UP);

        return new HistorySummary(sales, paid, average, cancelled, total);
    }

    /** Missing bounds mean "no limit", so they widen to the extremes. */
    private static LocalDateTime startOf(LocalDate date) {
        return date == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : date.atStartOfDay();
    }

    private static LocalDateTime endOf(LocalDate date) {
        return date == null ? LocalDateTime.of(2999, 12, 31, 23, 59, 59) : date.atTime(LocalTime.MAX);
    }

    /** The open bill at a table, if there is one — used when the POS reloads. */
    @Transactional(readOnly = true)
    public OrderResponse openForTable(Long tableId) {
        return orderRepository.findByTableIdAndStatus(tableId, OrderStatus.OPEN).stream()
                .findFirst()
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("error.order.noOpenOrder", tableId));
    }

    /* ===================================================================== */
    /* Internals                                                             */
    /* ===================================================================== */

    Order find(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.order", id));
    }

    private Order findEditable(Long id) {
        Order order = find(id);
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new BadRequestException(
                    "error.order.notEditable", order.getInvoiceNo(), order.getStatus());
        }
        return order;
    }

    void freeTable(Order order) {
        DiningTable table = order.getTable();
        if (table == null) return;

        // Only release the table when nothing else is still open on it.
        boolean stillBusy = orderRepository.findByTableIdAndStatus(table.getId(), OrderStatus.OPEN)
                .stream().anyMatch(o -> !o.getId().equals(order.getId()));
        if (!stillBusy) {
            table.setStatus(TableStatus.FREE);
            tableRepository.save(table);
        }
    }

    private String nextInvoiceNo() {
        // From a DB sequence, not count()+1 — two cashiers ringing up at the same
        // moment would otherwise generate the same invoice number.
        Long seq = orderRepository.nextInvoiceSequence();
        return "%s%05d".formatted(settings.invoicePrefix(), seq);
    }

    /** Recomputes subtotal, VAT, total and the riel figure from the lines. */
    void recalculate(Order order) {
        BigDecimal subtotal = order.getItems().stream()
                .map(OrderItem::getLineTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal discount = order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount();
        if (discount.compareTo(subtotal) > 0) {
            throw new BadRequestException("error.order.discountTooLarge");
        }

        BigDecimal taxable = subtotal.subtract(discount);
        BigDecimal rate = order.getVatRate() == null ? BigDecimal.ZERO : order.getVatRate();
        BigDecimal vat = taxable.multiply(rate)
                .divide(BigDecimal.valueOf(100), MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal total = taxable.add(vat).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setVatAmount(vat);
        order.setTotal(total);
        // Riel has no minor unit, so the converted figure is a whole number.
        order.setTotalKhr(total.multiply(settings.khrRate()).setScale(0, RoundingMode.HALF_UP));
    }

    OrderResponse toResponse(Order o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getId(),
                        i.getProduct() != null ? i.getProduct().getId() : null,
                        i.getProductName(), i.getQty(), i.getUnitPrice(),
                        i.getLineTotal(), i.getNote()))
                .toList();

        return new OrderResponse(
                o.getId(), o.getInvoiceNo(),
                o.getTable() != null ? o.getTable().getId() : null,
                o.getTable() != null ? o.getTable().getName() : null,
                o.getCashier() != null ? o.getCashier().getId() : null,
                o.getCashier() != null ? o.getCashier().getUserNm() : null,
                o.getGuestCount(), items,
                o.getSubtotal(), o.getDiscount(), o.getVatRate(), o.getVatAmount(),
                o.getTotal(), o.getTotalKhr(),
                o.getPaymentMethod(), o.getAmountTendered(), o.getChangeAmount(),
                o.getStatus(), o.getRegDtm(), o.getPaidAt());
    }
}
