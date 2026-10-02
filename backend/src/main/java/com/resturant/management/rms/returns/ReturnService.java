package com.resturant.management.rms.returns;

import com.resturant.management.rms.catalog.Product;
import com.resturant.management.rms.common.Strings;
import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.ConflictException;
import com.resturant.management.rms.common.exception.ForbiddenException;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.customer.CustomerService;
import com.resturant.management.rms.order.*;
import com.resturant.management.rms.returns.dto.ReturnDtos.*;
import com.resturant.management.rms.setting.SettingService;
import com.resturant.management.rms.shift.CashShift;
import com.resturant.management.rms.shift.ShiftService;
import com.resturant.management.rms.stock.MovementType;
import com.resturant.management.rms.stock.StockLedger;
import com.resturant.management.rms.user.Role;
import com.resturant.management.rms.user.UserInfm;
import com.resturant.management.rms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Giving money back.
 *
 * <p>Five things happen together or not at all: the return document, a stock
 * movement per line, cash out of the drawer, a reversal against the payment,
 * and the customer's points taken back. ARCHITECTURE §4.2 asks for exactly
 * this, and the reason is that any one of them landing without the others
 * leaves the books saying something untrue — food back on the shelf that was
 * never refunded, or a drawer short by a figure no document explains.
 *
 * <p>The original sale is never touched. It is a printed record somebody may
 * be holding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnService {

    private static final int MONEY_SCALE = 2;

    private final SaleReturnRepository returns;
    private final OrderRepository orders;
    private final UserRepository users;
    private final StockLedger stockLedger;
    private final ShiftService shifts;
    private final CustomerService customers;
    private final SettingService settings;

    /* ===================================================================== */
    /* What is left to return                                                */
    /* ===================================================================== */

    /**
     * The lines of a sale with what remains returnable on each.
     *
     * <p>The subtraction lives here and only here. Returning three of a line of
     * two across two documents is invisible to any CHECK the database can
     * carry, so the client is told the remainder rather than computing it — and
     * {@link #create} recomputes it anyway, because a number that travelled to
     * a browser and back is a suggestion.
     */
    @Transactional(readOnly = true)
    public ReturnableOrder returnable(Long orderId) {
        Order order = paidOrder(orderId);
        Map<Long, BigDecimal> already = returnedByLine(orderId);

        List<ReturnableLine> lines = order.getItems().stream().map(item -> {
            BigDecimal returned = already.getOrDefault(item.getId(), BigDecimal.ZERO);
            return new ReturnableLine(
                    item.getId(),
                    item.getProduct() != null ? item.getProduct().getId() : null,
                    item.getProductName(),
                    item.getQty(),
                    returned,
                    item.getQty().subtract(returned),
                    item.getUnitPrice());
        }).toList();

        return new ReturnableOrder(
                order.getId(), order.getInvoiceNo(), order.getPaidAt(), order.getTotal(),
                order.singleMethod(),
                lines.stream().anyMatch(l -> l.remainingQty().signum() > 0),
                lines);
    }

    /* ===================================================================== */
    /* Making one                                                            */
    /* ===================================================================== */

    @Transactional
    public ReturnResponse create(CreateReturnRequest request, String username) {
        Order order = paidOrder(request.orderId());
        UserInfm actor = users.findByUserId(username)
                .orElseThrow(() -> NotFoundException.of("entity.user", username));

        Map<Long, OrderItem> lineById = new HashMap<>();
        order.getItems().forEach(i -> lineById.put(i.getId(), i));
        Map<Long, BigDecimal> already = returnedByLine(order.getId());

        List<SaleReturnItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (ReturnLineRequest line : request.lines()) {
            OrderItem sold = lineById.get(line.orderItemId());
            if (sold == null) {
                throw new BadRequestException("error.return.lineNotOnBill", order.getInvoiceNo());
            }
            if (items.stream().anyMatch(i -> i.getOrderItem().getId().equals(sold.getId()))) {
                throw new BadRequestException("error.return.lineTwice", sold.getProductName());
            }

            BigDecimal qty = scale(line.qty());
            BigDecimal remaining = sold.getQty()
                    .subtract(already.getOrDefault(sold.getId(), BigDecimal.ZERO));

            // 409 rather than 400: nothing about the request is malformed, it
            // conflicts with returns that already exist. API §6.3.
            if (qty.compareTo(remaining) > 0) {
                throw new ConflictException("error.return.exceedsSold",
                        sold.getProductName(), remaining);
            }

            // Priced at what was charged, not at what the dish costs today.
            BigDecimal lineTotal = scale(sold.getUnitPrice().multiply(qty));
            total = total.add(lineTotal);

            items.add(SaleReturnItem.builder()
                    .orderItem(sold)
                    .qty(qty)
                    .lineTotal(lineTotal)
                    .build());
        }

        if (total.signum() <= 0) {
            throw new BadRequestException("error.return.nothingToRefund");
        }

        /*
         * The refund is proportional to the lines, which is not the same as
         * their price: a bill carries VAT and may carry a discount, and giving
         * back the gross line total would hand over tax that was never charged
         * on it. The ratio of line totals to the bill's subtotal is what the
         * customer actually paid for those dishes.
         */
        BigDecimal refund = scale(total.multiply(ratioOfBill(order)));
        PaymentMethod method = request.refundMethod() != null
                ? request.refundMethod()
                : originalMethod(order);

        requireApproval(refund, actor);

        SaleReturn saleReturn = SaleReturn.builder()
                .returnNo(nextReturnNo())
                .order(order)
                .reason(request.reason().trim())
                .total(refund)
                .refundMethod(method)
                .approvedBy(needsApproval(refund) ? actor : null)
                .build();
        items.forEach(saleReturn::addItem);

        // ---- and everything that follows from it, in this transaction ----

        CashShift shift = null;
        if (method == PaymentMethod.CASH) {
            // Cash out of a drawer nobody opened cannot be accounted for, so
            // this is the same gate the POS has — and the same reason for it.
            shift = shifts.requireOpenShift(username);
            saleReturn.setShift(shift);
        }

        returns.save(saleReturn);

        restock(saleReturn, username);
        reversePayment(order, refund, method);
        if (shift != null) {
            shifts.recordRefund(shift, refund, saleReturn.getReturnNo(), saleReturn.getId(), username);
        }
        customers.reverseEarned(order, refund, username);

        log.info("Return {} against {} — {} by {} ({} lines){}",
                saleReturn.getReturnNo(), order.getInvoiceNo(), refund, method,
                items.size(), saleReturn.getApprovedBy() != null ? ", approved" : "");

        return toResponse(saleReturn);
    }

    /* ===================================================================== */
    /* Reads                                                                 */
    /* ===================================================================== */

    @Transactional(readOnly = true)
    public Page<ReturnResponse> list(Long orderId, Pageable pageable) {
        Page<SaleReturn> page = orderId == null
                ? returns.findAllByOrderByIdDesc(pageable)
                : returns.findByOrderIdOrderByIdDesc(orderId, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReturnResponse get(Long id) {
        return toResponse(returns.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.return", id)));
    }

    /* ===================================================================== */
    /* The four things a return sets off                                     */
    /* ===================================================================== */

    /** Food that came back goes back on the shelf, line by line. */
    private void restock(SaleReturn saleReturn, String by) {
        for (SaleReturnItem item : saleReturn.getItems()) {
            Product product = item.getOrderItem().getProduct();
            if (product == null) continue;   // the dish has since been deleted

            stockLedger.recordProduct(product, MovementType.RETURN, item.getQty(),
                    saleReturn.getReturnNo(), StockLedger.REF_RETURN, saleReturn.getId(), by);
        }
    }

    /**
     * Records the money going back against the bill's payments.
     *
     * <p>A REFUNDED row rather than a negative amount: {@code sale_payment}
     * refuses a negative, and a separate row says what happened in a way a
     * reversed sign does not. Only CAPTURED rows count towards a bill being
     * paid, so this cannot make a settled bill look unsettled.
     */
    private void reversePayment(Order order, BigDecimal refund, PaymentMethod method) {
        // Through the aggregate, not straight at the table. Saving the row on
        // its own leaves the Order's own list of payments stale for the rest
        // of the transaction, so the response to this very request would have
        // shown the bill without its reversal.
        order.addPayment(SalePayment.builder()
                .method(method)
                .amount(refund)
                .status(PaymentStatus.REFUNDED)
                .createdAt(LocalDateTime.now())
                .build());
        orders.save(order);
    }

    /* ===================================================================== */
    /* Internals                                                             */
    /* ===================================================================== */

    private Order paidOrder(Long id) {
        Order order = orders.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.order", id));
        if (order.getStatus() != OrderStatus.PAID) {
            // An open bill is edited, not returned; a cancelled one took no
            // money to give back.
            throw new BadRequestException("error.return.notPaid", order.getInvoiceNo());
        }
        return order;
    }

    private Map<Long, BigDecimal> returnedByLine(Long orderId) {
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Object[] row : returns.returnedByLine(orderId)) {
            map.put((Long) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    /**
     * What a dollar of line total is worth on the settled bill.
     *
     * <p>VAT and any discount are spread across the lines, so returning one
     * dish gives back its share of what was actually paid rather than its
     * menu price. A bill with no subtotal cannot be apportioned; one is all
     * that can be returned then, and it is the figure already charged.
     */
    private BigDecimal ratioOfBill(Order order) {
        BigDecimal subtotal = order.getSubtotal();
        if (subtotal == null || subtotal.signum() <= 0) return BigDecimal.ONE;
        return order.getTotal().divide(subtotal, 6, RoundingMode.HALF_UP);
    }

    /** However the bill was settled, or cash when it was split and nothing is obvious. */
    private PaymentMethod originalMethod(Order order) {
        PaymentMethod single = order.singleMethod();
        if (single != null) return single;
        return order.capturedPayments().stream()
                .max((a, b) -> a.getAmount().compareTo(b.getAmount()))
                .map(SalePayment::getMethod)
                .orElse(PaymentMethod.CASH);
    }

    private boolean needsApproval(BigDecimal refund) {
        BigDecimal threshold = settings.getDecimal(
                SettingService.RETURN_APPROVAL_THRESHOLD, new BigDecimal("20"));
        return refund.compareTo(threshold) > 0;
    }

    /**
     * SCREENS §3.4: above a threshold somebody senior signs for it.
     *
     * <p>Not because cashiers are dishonest. A refund is the one transaction
     * that takes money out with nothing coming in, and the record needs a name
     * against it — this is the cheapest place to capture one.
     */
    private void requireApproval(BigDecimal refund, UserInfm actor) {
        if (!needsApproval(refund)) return;
        // ADMIN alone for now. ERD's migration list has MANAGER arriving in the
        // role CHECKs with P1; this is the line to widen when it does.
        if (actor.getRole() == Role.ADMIN) return;

        throw new ForbiddenException("error.return.approvalRequired",
                settings.getDecimal(SettingService.RETURN_APPROVAL_THRESHOLD, new BigDecimal("20")));
    }

    private String nextReturnNo() {
        // From the sequence, like invoice numbers: two tills refunding at once
        // would otherwise be handed the same document number.
        return "%s%05d".formatted(
                settings.getString(SettingService.RETURN_PREFIX, "RET-"),
                returns.nextReturnSequence());
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private ReturnResponse toResponse(SaleReturn r) {
        List<ReturnItemResponse> items = r.getItems().stream()
                .map(i -> new ReturnItemResponse(
                        i.getId(),
                        i.getOrderItem().getId(),
                        i.getOrderItem().getProductName(),
                        i.getQty(),
                        i.getOrderItem().getUnitPrice(),
                        i.getLineTotal()))
                .toList();

        return new ReturnResponse(
                r.getId(), r.getReturnNo(),
                r.getOrder().getId(), r.getOrder().getInvoiceNo(),
                r.getTotal(), r.getRefundMethod(), r.getReason(),
                r.getRegId(), r.getRegDtm(),
                r.getApprovedBy() != null ? r.getApprovedBy().getUserNm() : null,
                r.getShift() != null ? r.getShift().getId() : null,
                items);
    }
}
