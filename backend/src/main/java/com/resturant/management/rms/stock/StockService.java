package com.resturant.management.rms.stock;

import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.stock.dto.StockDtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.resturant.management.rms.common.Strings.blankToNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockItemRepository stockRepository;
    private final StockMovementRepository movementRepository;

    /* ---- Reads ----------------------------------------------------------- */

    @Transactional(readOnly = true)
    public Page<StockItemResponse> search(String query, Pageable pageable) {
        return stockRepository.search(blankToNull(query), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public StockItemResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<StockItemResponse> lowStock() {
        return stockRepository.findLowStock().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public StockSummary summary() {
        return new StockSummary(
                stockRepository.count(),
                stockRepository.totalStockValue(),
                stockRepository.countLowStock(),
                stockRepository.countOutOfStock());
    }

    @Transactional(readOnly = true)
    public Page<MovementResponse> movements(Long stockItemId, Pageable pageable) {
        return movementRepository
                .findByStockItemIdOrderByCreatedAtDesc(stockItemId, pageable)
                .map(this::toMovement);
    }

    /* ---- Writes ---------------------------------------------------------- */

    @Transactional
    public StockItemResponse create(StockItemRequest request) {
        StockItem item = new StockItem();
        apply(item, request);
        return toResponse(stockRepository.save(item));
    }

    @Transactional
    public StockItemResponse update(Long id, StockItemRequest request) {
        StockItem item = find(id);
        apply(item, request);
        return toResponse(stockRepository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        StockItem item = find(id);
        // Movements reference the item, so removing it would orphan the audit
        // trail. Purchases reference it too and would fail on the FK anyway.
        if (movementRepository.countByStockItemId(id) > 0) {
            throw new BadRequestException("error.stock.hasMovements", item.getName());
        }
        stockRepository.delete(item);
    }

    /**
     * Applies a correction and records why.
     *
     * <p>The movement row is the audit trail: every change to a quantity leaves
     * one behind, so a discrepancy can always be traced.
     */
    @Transactional
    public StockItemResponse adjust(Long id, AdjustRequest request, String username) {
        StockItem item = find(id);

        BigDecimal current = item.getQty() == null ? BigDecimal.ZERO : item.getQty();
        BigDecimal delta = switch (request.type()) {
            case IN -> request.qty();
            case OUT, DAMAGED -> request.qty().negate();
        };

        BigDecimal after = current.add(delta);
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(
                    "error.stock.insufficient", request.qty(), item.getName(), current);
        }

        item.setQty(after);
        stockRepository.save(item);

        movementRepository.save(StockMovement.builder()
                .stockItem(item)
                .movementType(request.type())
                .qty(request.qty())
                .reason(request.reason())
                .createdBy(username)
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Stock '{}' {} {} → {} by {}",
                item.getName(), request.type(), request.qty(), after, username);
        return toResponse(item);
    }

    /**
     * Increases stock because goods were received, and records the movement.
     * Called by {@code PurchaseService} inside its own transaction.
     */
    @Transactional
    public void receive(StockItem item, BigDecimal qty, String reference, String username) {
        BigDecimal current = item.getQty() == null ? BigDecimal.ZERO : item.getQty();
        item.setQty(current.add(qty));
        stockRepository.save(item);

        movementRepository.save(StockMovement.builder()
                .stockItem(item)
                .movementType(MovementType.IN)
                .qty(qty)
                .reason("Received on " + reference)
                .createdBy(username)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /* ---- Helpers ---------------------------------------------------------- */

    public StockItem find(Long id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.stockItem", id));
    }

    private void apply(StockItem item, StockItemRequest r) {
        item.setName(r.name());
        item.setUnit(r.unit());
        item.setQty(r.qty() != null ? r.qty() : BigDecimal.ZERO);
        item.setMinQty(r.minQty() != null ? r.minQty() : BigDecimal.ZERO);
        item.setUnitCost(r.unitCost() != null ? r.unitCost() : BigDecimal.ZERO);
    }

    private StockItemResponse toResponse(StockItem s) {
        BigDecimal qty = s.getQty() == null ? BigDecimal.ZERO : s.getQty();
        BigDecimal cost = s.getUnitCost() == null ? BigDecimal.ZERO : s.getUnitCost();
        return new StockItemResponse(
                s.getId(), s.getName(), s.getUnit(), qty, s.getMinQty(), cost,
                qty.multiply(cost), s.isLowStock(), s.isOutOfStock());
    }

    private MovementResponse toMovement(StockMovement m) {
        return new MovementResponse(
                m.getId(),
                m.getStockItem() != null ? m.getStockItem().getId() : null,
                m.getStockItem() != null ? m.getStockItem().getName() : null,
                m.getMovementType(), m.getQty(), m.getReason(), m.getCreatedBy(), m.getCreatedAt());
    }
}
