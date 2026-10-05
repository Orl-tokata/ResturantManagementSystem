package com.resturant.management.rms.promotion;

import com.resturant.management.rms.catalog.CategoryRepository;
import com.resturant.management.rms.catalog.ProductRepository;
import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.order.Order;
import com.resturant.management.rms.order.OrderRepository;
import com.resturant.management.rms.promotion.dto.PromotionDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Managing the rules, and answering what a given bill qualifies for. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotions;
    private final PromotionEngine engine;
    private final OrderRepository orders;
    private final com.resturant.management.rms.order.OrderItemRepository orderItems;
    private final ProductRepository products;
    private final CategoryRepository categories;

    /* ===================================================================== */
    /* Managing                                                              */
    /* ===================================================================== */

    @Transactional(readOnly = true)
    public Page<PromotionResponse> list(Pageable pageable) {
        return promotions.findAllByOrderByStartsAtDesc(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PromotionResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public PromotionResponse create(PromotionRequest request) {
        Promotion promotion = new Promotion();
        apply(promotion, request);
        return toResponse(promotions.save(promotion));
    }

    @Transactional
    public PromotionResponse update(Long id, PromotionRequest request) {
        Promotion promotion = find(id);
        apply(promotion, request);
        return toResponse(promotions.save(promotion));
    }

    /**
     * Removes a rule.
     *
     * <p>Refused once a bill has used it: {@code orders.promotion_id} and
     * {@code order_item.promotion_id} point here, and a receipt that says
     * "Tuesday offer" should still be able to say which offer that was.
     * Switching it off is what ending a promotion means.
     */
    @Transactional
    public void delete(Long id) {
        Promotion promotion = find(id);
        // Both places a bill can point at a rule: a whole-bill discount sits
        // on the order, a line discount on the line.
        if (orders.countByPromotionId(id) > 0 || orderItems.countByPromotionId(id) > 0) {
            throw new BadRequestException("error.promotion.inUse", promotion.getName());
        }
        promotions.delete(promotion);
    }

    /* ===================================================================== */
    /* Asking                                                                */
    /* ===================================================================== */

    /**
     * What this bill qualifies for, as a list the screen can show.
     *
     * <p>Computed here and never by the client — API §6.6. A till that decides
     * its own discount decides its own price. What actually comes off is
     * written by {@link PromotionEngine} inside the recalculation, and this
     * reads the result of that rather than guessing at it separately.
     */
    @Transactional(readOnly = true)
    public List<AppliedPromotion> applicableTo(Long orderId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> NotFoundException.of("entity.order", orderId));

        List<AppliedPromotion> applied = new java.util.ArrayList<>();

        if (order.getPromotionId() != null && order.getPromoDiscount().signum() > 0) {
            promotions.findById(order.getPromotionId()).ifPresent(p ->
                    applied.add(new AppliedPromotion(p.getId(), p.getName(), p.getScope(),
                            null, order.getPromoDiscount())));
        }

        order.getItems().stream()
                .filter(i -> i.getPromotionId() != null && i.getDiscountAmount().signum() > 0)
                .forEach(i -> promotions.findById(i.getPromotionId()).ifPresent(p ->
                        applied.add(new AppliedPromotion(p.getId(), p.getName(), p.getScope(),
                                i.getProductName(), i.getDiscountAmount()))));

        return applied;
    }

    /** Every rule running at this moment, for the admin screen's "live" badge. */
    @Transactional(readOnly = true)
    public List<PromotionResponse> live() {
        return engine.liveNow().stream().map(this::toResponse).toList();
    }

    /* ===================================================================== */
    /* Internals                                                             */
    /* ===================================================================== */

    private Promotion find(Long id) {
        return promotions.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.promotion", id));
    }

    private void apply(Promotion promotion, PromotionRequest request) {
        if (request.type() == PromotionType.BUY_X_GET_Y) {
            // SCREENS §3.5: not in Phase 1. The database allows the value so
            // that adding it later needs no migration, and this is what keeps
            // it from being half-built in the meantime.
            throw new BadRequestException("error.promotion.typeNotReady");
        }
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new BadRequestException("error.promotion.dates");
        }
        if (request.type() == PromotionType.PERCENT
                && request.value().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("error.promotion.percentTooLarge");
        }

        // The scope has to name something that exists, or the rule is one that
        // can never fire and nobody finds out until the day it should have.
        switch (request.scope()) {
            case ITEM -> {
                requireScopeId(request);
                products.findById(request.scopeId())
                        .orElseThrow(() -> NotFoundException.of("entity.product", request.scopeId()));
            }
            case CATEGORY -> {
                requireScopeId(request);
                categories.findById(request.scopeId())
                        .orElseThrow(() -> NotFoundException.of("entity.category", request.scopeId()));
            }
            case ORDER -> {
                if (request.scopeId() != null) {
                    throw new BadRequestException("error.promotion.scopeIdNotAllowed");
                }
            }
        }

        promotion.setName(request.name().trim());
        promotion.setType(request.type());
        promotion.setValue(request.value());
        promotion.setScope(request.scope());
        promotion.setScopeId(request.scopeId());
        promotion.setMinAmount(request.minAmount());
        promotion.setStartsAt(request.startsAt());
        promotion.setEndsAt(request.endsAt());
        promotion.setTimeFrom(request.timeFrom());
        promotion.setTimeTo(request.timeTo());
        promotion.setActYn(Boolean.FALSE.equals(request.active()) ? "N" : "Y");
    }

    private void requireScopeId(PromotionRequest request) {
        if (request.scopeId() == null) {
            throw new BadRequestException("error.promotion.scopeIdRequired");
        }
    }

    private PromotionResponse toResponse(Promotion p) {
        String scopeName = switch (p.getScope()) {
            case ITEM -> products.findById(p.getScopeId())
                    .map(x -> x.getName()).orElse(null);
            case CATEGORY -> categories.findById(p.getScopeId())
                    .map(x -> x.getName()).orElse(null);
            case ORDER -> null;
        };

        return new PromotionResponse(
                p.getId(), p.getName(), p.getType(), p.getValue(),
                p.getScope(), p.getScopeId(), scopeName,
                p.getMinAmount(), p.getStartsAt(), p.getEndsAt(),
                p.getTimeFrom(), p.getTimeTo(),
                "Y".equals(p.getActYn()),
                // Whether it would fire right now, which is the thing an owner
                // actually wants to know when looking at the list.
                "Y".equals(p.getActYn())
                        && !p.getStartsAt().isAfter(java.time.LocalDateTime.now())
                        && !p.getEndsAt().isBefore(java.time.LocalDateTime.now())
                        && p.appliesAt(java.time.LocalTime.now()));
    }

    /** Rounding used wherever a percentage meets money here. */
    static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
