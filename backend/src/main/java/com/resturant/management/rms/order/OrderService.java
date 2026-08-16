package com.resturant.management.rms.order;

import com.resturant.management.rms.catalog.Product;
import com.resturant.management.rms.catalog.ProductRepository;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
                .orElseThrow(() -> NotFoundException.of("Table", request.tableId()));

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
                    .orElseThrow(() -> NotFoundException.of("Product", line.productId()));

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

    /** The open bill at a table, if there is one — used when the POS reloads. */
    @Transactional(readOnly = true)
    public OrderResponse openForTable(Long tableId) {
        return orderRepository.findByTableIdAndStatus(tableId, OrderStatus.OPEN).stream()
                .findFirst()
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("No open order at table " + tableId));
    }

    /* ===================================================================== */
    /* Internals                                                             */
    /* ===================================================================== */

    Order find(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Order", id));
    }

    private Order findEditable(Long id) {
        Order order = find(id);
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new BadRequestException(
                    "Order %s is %s and can no longer be changed"
                            .formatted(order.getInvoiceNo(), order.getStatus()));
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
            throw new BadRequestException("Discount cannot exceed the subtotal");
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
