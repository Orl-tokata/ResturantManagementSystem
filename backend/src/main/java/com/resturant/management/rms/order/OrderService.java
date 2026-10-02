package com.resturant.management.rms.order;

import com.resturant.management.rms.catalog.Product;
import com.resturant.management.rms.stock.MovementType;
import com.resturant.management.rms.stock.StockLedger;
import com.resturant.management.rms.catalog.ProductRepository;
import com.resturant.management.rms.common.Strings;
import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.dining.DiningTable;
import com.resturant.management.rms.dining.DiningTableRepository;
import com.resturant.management.rms.dining.TableStatus;
import com.resturant.management.rms.khqr.BakongClient;
import com.resturant.management.rms.khqr.Khqr;
import com.resturant.management.rms.khqr.KhqrGenerator;
import com.resturant.management.rms.khqr.KhqrMerchant;
import com.resturant.management.rms.khqr.KhqrProperties;
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
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int MONEY_SCALE = 2;

    private final OrderRepository orderRepository;
    private final SalePaymentRepository paymentRepository;
    private final StockLedger stockLedger;
    private final ProductRepository productRepository;
    private final DiningTableRepository tableRepository;
    private final UserRepository userRepository;
    private final SettingService settings;
    private final KhqrGenerator khqrGenerator;
    private final BakongClient bakong;
    private final KhqrProperties khqrProperties;

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
                    // Copied now, for the same reason the price is: the margin
                    // on this sale is the margin that was made, and repricing
                    // the dish next month must not rewrite it. Recorded, so
                    // not estimated.
                    .unitCost(product.getCost() == null ? BigDecimal.ZERO : product.getCost())
                    .costEstimated(false)
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

        capture(order, request.tenders());
        close(order);

        Order saved = orderRepository.save(order);
        log.info("Paid {} — {} by {}", saved.getInvoiceNo(), saved.getTotal(),
                saved.capturedPayments().stream().map(p -> p.getMethod().name()).toList());
        return toResponse(saved);
    }

    /**
     * Turns what the cashier took into payment rows, and insists they add up.
     *
     * <p>The tenders must cover the bill exactly. Under it is not a settled
     * bill — that is a partial payment, which nothing in the system can
     * represent yet and which silently marking PAID would hide. Over it is not
     * a payment either: money above the total is change, and change comes off
     * the cash line.
     */
    private void capture(Order order, List<TenderRequest> tenders) {
        BigDecimal outstanding = order.getTotal().subtract(order.paidAmount());
        LocalDateTime now = LocalDateTime.now();

        // Built first and attached at the end. A bill that was not covered
        // must leave no trace of the attempt on the aggregate, and the way to
        // be sure of that is not to touch it until the arithmetic is known to
        // work — the surrounding transaction would undo it, but relying on a
        // rollback to hide a half-settled bill is a weaker guarantee than
        // never building one.
        List<SalePayment> captured = new java.util.ArrayList<>();

        for (TenderRequest t : tenders) {
            // Null means the rest of the bill, which is what a single tender
            // always means — see TenderRequest.
            BigDecimal amount = t.amount() == null
                    ? outstanding
                    : t.amount().setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            if (amount.signum() <= 0) {
                throw new BadRequestException("error.order.paymentNotPositive");
            }
            if (amount.compareTo(outstanding) > 0) {
                throw new BadRequestException("error.order.paymentTooLarge", amount, outstanding);
            }

            BigDecimal tendered = null;
            BigDecimal change = null;

            // Cash is the only method where the amount handed over is
            // meaningful, and the only one that can give change back. A card
            // for $20 of a $50 bill is a payment of $20, not $20 tendered.
            if (t.method() == PaymentMethod.CASH) {
                tendered = t.tendered();
                if (tendered == null) {
                    throw new BadRequestException("error.order.tenderedRequired");
                }
                if (tendered.compareTo(amount) < 0) {
                    throw new BadRequestException("error.order.tenderedShort", tendered, amount);
                }
                change = tendered.subtract(amount).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }

            captured.add(SalePayment.builder()
                    .method(t.method())
                    .amount(amount)
                    .amountKhr(toKhr(order, amount))
                    .tendered(tendered)
                    .changeAmount(change)
                    .reference(Strings.blankToNull(t.reference()))
                    .status(PaymentStatus.CAPTURED)
                    .createdAt(now)
                    .build());

            outstanding = outstanding.subtract(amount);
        }

        if (outstanding.signum() > 0) {
            throw new BadRequestException("error.order.paymentShort",
                    order.getTotal().subtract(outstanding), order.getTotal());
        }

        captured.forEach(order::addPayment);
    }

    /** Marks a covered bill settled: stock leaves, the table is released. */
    private void close(Order order) {
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        decrementStock(order);
        freeTable(order);
    }

    /**
     * Riel has no minor unit, so a converted figure is a whole number.
     *
     * <p>At the bill's own rate, not today's: the payments on a bill have to
     * add up to it in both currencies, and reading the live setting here would
     * break that the moment the rate moved between a split's two tenders.
     */
    private BigDecimal toKhr(Order order, BigDecimal usd) {
        BigDecimal rate = order.getFxRateKhr() != null ? order.getFxRateKhr() : settings.khrRate();
        return usd.multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Reduces each sold product's stock.
     *
     * <p>Stock is allowed to go negative rather than blocking the sale: the food
     * has already left the kitchen by the time the bill is settled, so refusing
     * payment would be the wrong answer. A negative figure is a signal for the
     * stock screen, not a reason to fail here.
     *
     * <p>Each line leaves a movement naming the bill that caused it, so a count
     * that looks wrong can be traced to the sales that made it so. Until V11
     * this wrote nothing at all, and the question had no answer.
     *
     * <p>Still no recipe table: a dish decrements its own figure, not the
     * ingredients behind it — PROJECT-SPEC.md §12.
     */
    private void decrementStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (product == null) continue;

            // Inside the settle transaction, so a bill and the stock it moved
            // commit together or not at all.
            stockLedger.recordProduct(product, MovementType.SALE, item.getQty(),
                    order.getInvoiceNo(), StockLedger.REF_ORDER, order.getId(), order.getModId());
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
    /**
     * The payment methods this till can actually carry out.
     *
     * <p>KHQR drops out when no Bakong account is configured. Offering a method
     * the till will refuse is a small cruelty: the cashier taps it in front of
     * a waiting customer and only then finds out. Cash, card and transfer are
     * always available because settling them needs nothing from this software
     * beyond recording what happened.
     */
    public List<PaymentMethod> availablePaymentMethods() {
        return Arrays.stream(PaymentMethod.values())
                .filter(m -> m != PaymentMethod.KHQR || khqrProperties.canGenerate())
                .toList();
    }

    /* ===================================================================== */
    /* KHQR                                                                  */
    /* ===================================================================== */

    /**
     * Produces the code for a bill and parks the order until it is paid.
     *
     * <p>The order moves to AWAITING_PAYMENT rather than PAID. That is the
     * point of the whole integration: a code on a screen is a request for
     * money, and the only thing that turns it into a payment is the bank
     * saying so.
     *
     * <p>Re-entrant on purpose. A till that is refreshed, or a cashier who
     * navigates away and back, must be shown the same code — generating a new
     * one would change the md5 and orphan a customer who had already scanned
     * the old one, leaving a real payment that this order can never match.
     */
    @Transactional
    public KhqrResponse startKhqrPayment(Long orderId) {
        if (!khqrProperties.canGenerate()) {
            throw new BadRequestException("error.order.khqrUnavailable");
        }

        Order order = find(orderId);
        if (order.getStatus() == OrderStatus.PAID) {
            throw new BadRequestException("error.order.alreadyPaid", order.getInvoiceNo());
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("error.order.notEditable",
                    order.getInvoiceNo(), order.getStatus());
        }
        if (order.getItems().isEmpty()) {
            throw new BadRequestException("error.order.emptyBill");
        }

        LocalDateTime now = LocalDateTime.now();
        boolean live = order.getKhqrMd5() != null
                && order.getKhqrExpiresAt() != null
                && order.getKhqrExpiresAt().isAfter(now);

        if (live) {
            return new KhqrResponse(order.getKhqrPayload(), order.getTotal().toPlainString(),
                    settings.getString("currency.base", "USD"),
                    order.getKhqrExpiresAt(), khqrProperties.canVerify());
        }

        // The amount is fixed at this moment, so the bill must stop moving.
        recalculate(order);

        String currency = settings.getString("currency.base", "USD");
        BigDecimal amount = "KHR".equalsIgnoreCase(currency)
                ? order.getTotalKhr()
                : order.getTotal();

        Khqr qr = khqrGenerator.generate(merchant(), amount, currency, order.getInvoiceNo());

        order.setKhqrMd5(qr.md5());
        order.setKhqrPayload(qr.payload());
        order.setKhqrExpiresAt(now.plus(khqrProperties.expiry()));
        order.setStatus(OrderStatus.AWAITING_PAYMENT);

        // Any earlier code for this bill is now unreachable: its md5 is gone
        // from the order, so nothing can ever match it to money. That is a
        // failed attempt, and recording it is the point — "three codes shown,
        // one paid" had no answer at all before this table existed.
        failPending(order, "replaced by a new code");
        order.addPayment(SalePayment.builder()
                .method(PaymentMethod.KHQR)
                .amount(order.getTotal())
                .amountKhr(order.getTotalKhr())
                .khqrMd5(qr.md5())
                .status(PaymentStatus.PENDING)
                .createdAt(now)
                .build());
        orderRepository.save(order);

        log.info("KHQR shown for {} — {} {} (md5 {})",
                order.getInvoiceNo(), qr.amount(), qr.currency(), qr.md5());

        return new KhqrResponse(qr.payload(), qr.amount(), qr.currency(),
                order.getKhqrExpiresAt(), khqrProperties.canVerify());
    }

    /**
     * Asks Bakong whether the outstanding code was paid, and closes the bill if
     * it was.
     *
     * <p>Only PAID changes anything. An unreachable bank or an empty answer
     * leaves the order exactly where it is — reporting "not paid" on a network
     * error would be a guess, and reporting "paid" would be a gift.
     */
    @Transactional
    public KhqrStatusResponse checkKhqrPayment(Long orderId) {
        Order order = find(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            return new KhqrStatusResponse("PAID", null, toResponse(order));
        }
        if (order.getKhqrMd5() == null) {
            throw new BadRequestException("error.order.noKhqr", order.getInvoiceNo());
        }

        BakongClient.PaymentStatus status = bakong.check(order.getKhqrMd5());

        if (status.state() == BakongClient.PaymentStatus.State.PAID) {
            settleKhqr(order, status.payer(), status.reference());
            return new KhqrStatusResponse("PAID", null, toResponse(order));
        }

        // Expiry is only reported once the bank has been asked and said no. A
        // customer who paid on the last second should not lose the payment to a
        // clock.
        if (order.getKhqrExpiresAt() != null
                && order.getKhqrExpiresAt().isBefore(LocalDateTime.now())
                && status.state() == BakongClient.PaymentStatus.State.NOT_PAID) {
            return new KhqrStatusResponse("EXPIRED", status.detail(), toResponse(order));
        }

        return new KhqrStatusResponse(status.state().name(), status.detail(), toResponse(order));
    }

    /**
     * Gives up on an outstanding code and returns the bill to the floor, so it
     * can be settled in cash instead.
     */
    @Transactional
    public OrderResponse abandonKhqrPayment(Long orderId) {
        Order order = find(orderId);
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new BadRequestException("error.order.notAwaiting", order.getInvoiceNo());
        }

        order.setKhqrMd5(null);
        order.setKhqrPayload(null);
        order.setKhqrExpiresAt(null);
        order.setStatus(OrderStatus.OPEN);
        failPending(order, "abandoned by the cashier");
        return toResponse(orderRepository.save(order));
    }

    /** Closes a bill that Bakong has confirmed. */
    private void settleKhqr(Order order, String payer, String reference) {
        LocalDateTime now = LocalDateTime.now();

        // The pending row becomes the payment. created_at moves to now because
        // the column is when the money arrived, not when the code was drawn,
        // and a drawer is counted by the former.
        SalePayment pending = pendingKhqr(order);
        if (pending != null) {
            pending.setStatus(PaymentStatus.CAPTURED);
            pending.setReference(Strings.blankToNull(reference));
            pending.setAmount(order.getTotal());
            pending.setAmountKhr(order.getTotalKhr());
            pending.setCreatedAt(now);
        } else {
            // A code generated before V12 has no pending row to promote. The
            // money still arrived, so it still needs a payment.
            order.addPayment(SalePayment.builder()
                    .method(PaymentMethod.KHQR)
                    .amount(order.getTotal())
                    .amountKhr(order.getTotalKhr())
                    .khqrMd5(order.getKhqrMd5())
                    .reference(Strings.blankToNull(reference))
                    .status(PaymentStatus.CAPTURED)
                    .createdAt(now)
                    .build());
        }

        order.setKhqrPayer(payer);
        order.setKhqrReference(reference);
        order.setKhqrExpiresAt(null);

        close(order);
        orderRepository.save(order);

        log.info("KHQR settled for {} — {} paid by {} (ref {})",
                order.getInvoiceNo(), order.getTotal(), payer, reference);
    }

    /** The code currently on screen for this bill, if one was recorded. */
    private SalePayment pendingKhqr(Order order) {
        return order.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    /**
     * Closes off an outstanding code that produced no money.
     *
     * <p>FAILED rather than deleting the row: the attempt happened, and a till
     * that showed four codes to settle one sale is worth being able to see.
     */
    private void failPending(Order order, String why) {
        SalePayment pending = pendingKhqr(order);
        if (pending == null) return;
        pending.setStatus(PaymentStatus.FAILED);
        pending.setReference(why);
        log.info("KHQR attempt on {} ended without payment — {}", order.getInvoiceNo(), why);
    }

    private KhqrMerchant merchant() {
        String name = khqrProperties.merchantName() != null && !khqrProperties.merchantName().isBlank()
                ? khqrProperties.merchantName()
                : settings.getString("restaurant.nameEn", "Restaurant");

        return new KhqrMerchant(
                khqrProperties.accountId(),
                name,
                khqrProperties.city(),
                khqrProperties.acquiringBank(),
                khqrProperties.storeLabel(),
                khqrProperties.terminalLabel());
    }

    /* ===================================================================== */

    Order find(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.order", id));
    }

    /**
     * Adds one field up across the captured payments, or null when none of them
     * carried it. A card sale has no tender, and a zero there would read as
     * "nothing was handed over" rather than "the question does not apply".
     */
    private BigDecimal sum(List<SalePayment> payments,
                           java.util.function.Function<SalePayment, BigDecimal> field) {
        List<BigDecimal> values = payments.stream()
                .map(field)
                .filter(java.util.Objects::nonNull)
                .toList();
        return values.isEmpty() ? null : values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
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

        BigDecimal khrRate = settings.khrRate();

        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setVatAmount(vat);
        order.setTotal(total);
        // Riel has no minor unit, so the converted figure is a whole number.
        order.setTotalKhr(total.multiply(khrRate).setScale(0, RoundingMode.HALF_UP));

        // The rate goes on the bill beside the figure it produced. The setting
        // can be corrected tomorrow; what this receipt was converted at cannot
        // change afterwards, and dividing total_khr by total to find out is
        // not the same as having recorded it.
        order.setFxRateKhr(khrRate);
    }

    OrderResponse toResponse(Order o) {
        List<PaymentResponse> payments = o.getPayments().stream()
                .map(p -> new PaymentResponse(
                        p.getId(), p.getMethod(), p.getAmount(), p.getAmountKhr(),
                        p.getTendered(), p.getChangeAmount(), p.getReference(),
                        p.getStatus(), p.getCreatedAt()))
                .toList();
        List<SalePayment> captured = o.capturedPayments();

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
                o.getTotal(), o.getTotalKhr(), o.getFxRateKhr(),
                payments,
                // Derived, not stored: a screen that shows one payment line
                // gets one, and a split bill shows no single method rather than
                // picking a winner out of two.
                o.singleMethod(),
                sum(captured, SalePayment::getTendered),
                sum(captured, SalePayment::getChangeAmount),
                o.getStatus(), o.getRegDtm(), o.getPaidAt());
    }
}
