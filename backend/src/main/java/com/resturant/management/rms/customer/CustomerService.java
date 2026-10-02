package com.resturant.management.rms.customer;

import com.resturant.management.rms.common.Strings;
import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.customer.dto.CustomerDtos.*;
import com.resturant.management.rms.order.Order;
import com.resturant.management.rms.order.OrderRepository;
import com.resturant.management.rms.setting.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Customers, and the points they have.
 *
 * <p>The balance is never stored. Every read sums the ledger, which costs one
 * indexed aggregate and removes the whole class of bug where a cached total
 * and its rows disagree — see V15.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final int POINTS_SCALE = 2;

    private final CustomerRepository customers;
    private final LoyaltyRepository loyalty;
    private final OrderRepository orders;
    private final SettingService settings;

    /* ===================================================================== */
    /* Reads                                                                 */
    /* ===================================================================== */

    @Transactional(readOnly = true)
    public Page<CustomerResponse> search(String q, Pageable pageable) {
        // Normalised here rather than with `:q IS NULL OR ...`, which H2
        // accepts and PostgreSQL refuses — the bug that reached production
        // once already (PLAN.md P0d).
        Page<Customer> page = customers.search(q == null ? "" : q.trim(), pageable);
        return withTotals(page);
    }

    /**
     * The till's lookup: one exact phone number.
     *
     * <p>Returns a list because the column is not unique. Two people sharing a
     * number is ordinary here, and silently returning the first would attach
     * the bill to whichever of them was created first.
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> lookup(String phone) {
        String trimmed = phone == null ? "" : phone.trim();
        if (trimmed.isEmpty()) return List.of();
        return customers.findByPhoneOrderByIdAsc(trimmed).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public Page<LoyaltyResponse> ledger(Long id, Pageable pageable) {
        find(id);   // 404 for an unknown customer rather than an empty page
        return loyalty.findByCustomerIdOrderByIdDesc(id, pageable).map(t -> new LoyaltyResponse(
                t.getId(), t.getType(), t.getPoints(),
                t.getOrder() != null ? t.getOrder().getId() : null,
                t.getOrder() != null ? t.getOrder().getInvoiceNo() : null,
                t.getNote(), t.getCreatedBy(), t.getCreatedAt()));
    }

    /* ===================================================================== */
    /* Writes                                                                */
    /* ===================================================================== */

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer customer = Customer.builder()
                .code(nextCode())
                .name(request.name().trim())
                .build();
        apply(customer, request);
        return toResponse(customers.save(customer));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer customer = find(id);
        customer.setName(request.name().trim());
        apply(customer, request);
        return toResponse(customers.save(customer));
    }

    /**
     * Removes a customer who has no history.
     *
     * <p>Once they have bills or points, deleting them would orphan a receipt
     * and silently change a balance. Deactivating says the same thing without
     * rewriting what happened.
     */
    @Transactional
    public void delete(Long id) {
        Customer customer = find(id);
        if (!loyalty.findByCustomerIdOrderByIdDesc(id, Pageable.ofSize(1)).isEmpty()) {
            throw new BadRequestException("error.customer.hasHistory");
        }
        if (orders.countByCustomerId(id) > 0) {
            throw new BadRequestException("error.customer.hasHistory");
        }
        customers.delete(customer);
    }

    /** A manager correcting a balance. Always with a reason — that is the point. */
    @Transactional
    public LoyaltyResponse adjust(Long id, LoyaltyAdjustRequest request, String by) {
        Customer customer = find(id);
        if (request.points().compareTo(BigDecimal.ZERO) == 0) {
            throw new BadRequestException("error.loyalty.zeroAdjustment");
        }

        LoyaltyTransaction row = loyalty.save(LoyaltyTransaction.builder()
                .customer(customer)
                .type(LoyaltyType.ADJUST)
                .points(scale(request.points()))
                .note(request.note().trim())
                .createdBy(by)
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Loyalty adjusted for {} by {}: {} ({})",
                customer.getCode(), by, row.getPoints(), row.getNote());
        return new LoyaltyResponse(row.getId(), row.getType(), row.getPoints(),
                null, null, row.getNote(), row.getCreatedBy(), row.getCreatedAt());
    }

    /* ===================================================================== */
    /* Earning                                                               */
    /* ===================================================================== */

    /**
     * Gives a settled bill's points to whoever it belongs to.
     *
     * <p>Called from the settlement transaction, so the points and the sale
     * commit together or not at all. Silent when the bill has no customer,
     * which is most of them.
     *
     * <p>Earning twice for one meal is permanent, because the ledger is the
     * balance. The unique index on {@code (order_id, type)} is what actually
     * prevents it; this check is so the second attempt reads as a no-op rather
     * than a constraint violation in the middle of a payment.
     */
    @Transactional
    public void earn(Order order) {
        Customer customer = order.getCustomer();
        if (customer == null) return;

        BigDecimal rate = settings.getDecimal(SettingService.POINTS_PER_USD, BigDecimal.ONE);
        if (rate.signum() <= 0) return;

        BigDecimal points = scale(order.getTotal().multiply(rate));
        if (points.signum() <= 0) return;

        if (loyalty.existsByOrderIdAndType(order.getId(), LoyaltyType.EARN)) {
            log.debug("{} has already earned points; not paying twice", order.getInvoiceNo());
            return;
        }

        loyalty.save(LoyaltyTransaction.builder()
                .customer(customer)
                .order(order)
                .type(LoyaltyType.EARN)
                .earnOrderId(order.getId())
                .points(points)
                .note(order.getInvoiceNo())
                .createdBy(order.getModId())
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * Takes back the share of a meal's points that was refunded.
     *
     * <p>Proportional to what the bill earned rather than recalculated from
     * the refund: the rate may have moved since, and the customer should lose
     * exactly the fraction of what they were given, not what that money would
     * earn today.
     *
     * <p>Silent when the bill earned nothing — an anonymous sale, or one
     * settled while earning was switched off.
     */
    @Transactional
    public void reverseEarned(Order order, BigDecimal refunded, String by) {
        Customer customer = order.getCustomer();
        if (customer == null || refunded == null || refunded.signum() <= 0) return;

        BigDecimal earned = loyalty.earnedFor(order.getId());
        if (earned.signum() <= 0) return;

        BigDecimal total = order.getTotal();
        if (total == null || total.signum() <= 0) return;

        // Capped: a refund can never take back more than the meal gave.
        BigDecimal share = refunded.min(total).divide(total, 6, RoundingMode.HALF_UP);
        BigDecimal points = scale(earned.multiply(share));
        if (points.signum() <= 0) return;

        loyalty.save(LoyaltyTransaction.builder()
                .customer(customer)
                .order(order)
                .type(LoyaltyType.REVERSE)
                .points(points.negate())
                .note(order.getInvoiceNo())
                .createdBy(by)
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Reversed {} points for {} on {}", points, customer.getCode(), order.getInvoiceNo());
    }

    /* ===================================================================== */
    /* Internals                                                             */
    /* ===================================================================== */

    public Customer find(Long id) {
        return customers.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.customer", id));
    }

    private void apply(Customer customer, CustomerRequest request) {
        customer.setPhone(Strings.blankToNull(request.phone()));
        customer.setEmail(Strings.blankToNull(request.email()));
        customer.setBirthDate(request.birthDate());
        customer.setNote(Strings.blankToNull(request.note()));
    }

    private String nextCode() {
        // From the sequence, not count()+1: two people registering at once
        // would otherwise be handed the same code.
        return "%s%05d".formatted(
                settings.getString(SettingService.CUSTOMER_PREFIX, "C-"),
                customers.nextCodeSequence());
    }

    private CustomerResponse toResponse(Customer c) {
        return withTotals(new org.springframework.data.domain.PageImpl<>(List.of(c)))
                .getContent().get(0);
    }

    /**
     * Adds the points and the spend to a page of customers in two queries
     * rather than two per row.
     */
    private Page<CustomerResponse> withTotals(Page<Customer> page) {
        List<Long> ids = page.getContent().stream().map(Customer::getId).toList();
        if (ids.isEmpty()) return page.map(c -> plain(c, BigDecimal.ZERO));

        Map<Long, BigDecimal> points = loyalty.balancesOf(ids).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> scale((BigDecimal) r[1])));

        Map<Long, Object[]> spend = orders.customerTotals(ids).stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> r));

        return page.map(c -> {
            Object[] row = spend.get(c.getId());
            return new CustomerResponse(
                    c.getId(), c.getCode(), c.getName(), c.getPhone(), c.getEmail(),
                    c.getBirthDate(), c.getNote(),
                    points.getOrDefault(c.getId(), BigDecimal.ZERO),
                    row == null ? BigDecimal.ZERO : scale((BigDecimal) row[1]),
                    row == null ? 0L : ((Number) row[2]).longValue(),
                    row == null ? null : (LocalDateTime) row[3]);
        });
    }

    private CustomerResponse plain(Customer c, BigDecimal points) {
        return new CustomerResponse(c.getId(), c.getCode(), c.getName(), c.getPhone(),
                c.getEmail(), c.getBirthDate(), c.getNote(), points, BigDecimal.ZERO, 0L, null);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(POINTS_SCALE, RoundingMode.HALF_UP);
    }
}
