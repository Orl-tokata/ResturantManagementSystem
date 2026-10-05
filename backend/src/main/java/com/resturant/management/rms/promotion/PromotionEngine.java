package com.resturant.management.rms.promotion;

import com.resturant.management.rms.catalog.Product;
import com.resturant.management.rms.order.Order;
import com.resturant.management.rms.order.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Works out what comes off a bill, and writes it onto the bill.
 *
 * <p>Called from inside {@code OrderService.recalculate}, so the discount is
 * recomputed every time the basket changes and again at settlement. API §6.6
 * is explicit that the client never decides this: {@code /applicable} exists so
 * a screen can <em>show</em> what will apply, and this is where it actually
 * does.
 *
 * <p>The figures are written onto the order and its lines rather than derived
 * on read. A rule can be edited or switched off next week and a receipt in
 * somebody's hand says what it says — the same argument that put
 * {@code unit_price} on the line and {@code fx_rate_khr} on the order.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionEngine {

    private static final int MONEY_SCALE = 2;

    private final PromotionRepository promotions;

    /**
     * What a bill qualifies for right now, without changing anything.
     *
     * <p>Time of day is read at the moment of asking. A bill opened before
     * happy hour and paid after it gets the rule, because the figure that
     * matters is the one computed at settlement — and that is this same code,
     * run again.
     */
    @Transactional(readOnly = true)
    public List<Promotion> liveNow() {
        LocalDateTime now = LocalDateTime.now();
        return promotions.liveAt(now).stream()
                .filter(p -> p.getType() != PromotionType.BUY_X_GET_Y)
                .filter(p -> p.appliesAt(now.toLocalTime()))
                .toList();
    }

    /**
     * Applies the best rule to each line and to the bill, and returns what
     * came off in total.
     *
     * <p><b>One rule per line and one for the bill.</b> Stacking is where
     * promotion systems become unexplainable: two rules that each take 60% off
     * leave a line costing less than nothing, and the order they are applied in
     * changes the answer. Best-of is a rule a cashier can repeat to a
     * customer, and "best" means the largest discount — the one the customer
     * would have asked for.
     *
     * @return what the line-level rules took off, which the caller adds to the
     *         order-level figure
     */
    public BigDecimal apply(Order order, BigDecimal subtotal) {
        List<Promotion> live = liveNow();

        // Cleared first: a rule that no longer applies has to stop applying,
        // and the lines carry last time's answer until something says
        // otherwise.
        for (OrderItem item : order.getItems()) {
            item.setDiscountAmount(BigDecimal.ZERO);
            item.setPromotionId(null);
        }
        order.setPromoDiscount(BigDecimal.ZERO);
        order.setPromotionId(null);

        if (live.isEmpty()) return BigDecimal.ZERO;

        BigDecimal lineDiscounts = BigDecimal.ZERO;

        for (OrderItem item : order.getItems()) {
            Promotion best = null;
            BigDecimal bestOff = BigDecimal.ZERO;

            for (Promotion promotion : live) {
                if (!matches(promotion, item)) continue;

                BigDecimal lineTotal = item.getLineTotal() == null
                        ? BigDecimal.ZERO
                        : item.getLineTotal();
                if (belowFloor(promotion, lineTotal)) continue;

                BigDecimal off = promotion.discountOn(lineTotal, MONEY_SCALE, RoundingMode.HALF_UP);
                if (off.compareTo(bestOff) > 0) {
                    best = promotion;
                    bestOff = off;
                }
            }

            if (best != null) {
                item.setDiscountAmount(bestOff);
                item.setPromotionId(best.getId());
                lineDiscounts = lineDiscounts.add(bestOff);
            }
        }

        /*
         * A whole-bill rule is measured against what is left after the line
         * rules, not against the subtotal. Taking 10% of a figure that has
         * already been reduced is the arithmetic a customer expects; taking it
         * of the original would quietly hand out more than the rule says.
         */
        BigDecimal remaining = subtotal.subtract(lineDiscounts);
        Promotion bestOrder = null;
        BigDecimal bestOrderOff = BigDecimal.ZERO;

        for (Promotion promotion : live) {
            if (promotion.getScope() != PromotionScope.ORDER) continue;
            if (belowFloor(promotion, remaining)) continue;

            BigDecimal off = promotion.discountOn(remaining, MONEY_SCALE, RoundingMode.HALF_UP);
            if (off.compareTo(bestOrderOff) > 0) {
                bestOrder = promotion;
                bestOrderOff = off;
            }
        }

        if (bestOrder != null) {
            order.setPromoDiscount(bestOrderOff);
            order.setPromotionId(bestOrder.getId());
        }

        return lineDiscounts;
    }

    private boolean matches(Promotion promotion, OrderItem item) {
        Product product = item.getProduct();
        if (product == null) return false;

        return switch (promotion.getScope()) {
            case ITEM -> product.getId().equals(promotion.getScopeId());
            case CATEGORY -> product.getCategory() != null
                    && product.getCategory().getId().equals(promotion.getScopeId());
            case ORDER -> false;   // handled against the bill, not the line
        };
    }

    private boolean belowFloor(Promotion promotion, BigDecimal amount) {
        return promotion.getMinAmount() != null
                && amount.compareTo(promotion.getMinAmount()) < 0;
    }
}
