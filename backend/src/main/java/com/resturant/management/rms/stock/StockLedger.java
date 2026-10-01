package com.resturant.management.rms.stock;

import com.resturant.management.rms.catalog.Product;
import com.resturant.management.rms.catalog.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The only thing that changes a stock figure.
 *
 * <p>Before this, selling a dish subtracted from {@code product.stock_qty} and
 * wrote nothing down, so "why is this count wrong" had no answer anywhere in
 * the system. Routing every change through one place means the quantity and the
 * movement that caused it are written together or not at all.
 *
 * <p>The stored quantity is a cached balance, not the truth — the ledger is.
 * {@code balanceAfter} on each row is what lets the two be compared, and what
 * makes a wrong count traceable to the movement that made it wrong.
 *
 * <p>Nothing here refuses a movement for making a balance negative. A sale is
 * recorded after the food has left the kitchen, so refusing it would record a
 * lie to protect a number; the manual paths decide for themselves and
 * {@code StockService} does refuse. Negative is a signal for the stock screen,
 * not an error here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockLedger {

    private final StockMovementRepository movements;
    private final StockItemRepository stockItems;
    private final ProductRepository products;

    /** What caused a movement, for the {@code refType} column. */
    public static final String REF_ORDER = "ORDER";
    public static final String REF_PURCHASE = "PURCHASE";

    @Transactional
    public StockMovement recordProduct(Product product, MovementType type, BigDecimal qty,
                                       String reason, String refType, Long refId, String by) {
        require(qty);
        BigDecimal before = product.getStockQty() == null ? BigDecimal.ZERO : product.getStockQty();
        BigDecimal after = before.add(type.applyTo(qty));

        product.setStockQty(after);
        products.save(product);

        warnIfNegative(product.getName(), after);
        return write(StockMovement.builder().product(product), type, qty, reason, refType, refId, by, after);
    }

    @Transactional
    public StockMovement recordItem(StockItem item, MovementType type, BigDecimal qty,
                                    String reason, String refType, Long refId, String by) {
        require(qty);
        BigDecimal before = item.getQty() == null ? BigDecimal.ZERO : item.getQty();
        BigDecimal after = before.add(type.applyTo(qty));

        item.setQty(after);
        stockItems.save(item);

        warnIfNegative(item.getName(), after);
        return write(StockMovement.builder().stockItem(item), type, qty, reason, refType, refId, by, after);
    }

    private StockMovement write(StockMovement.StockMovementBuilder builder, MovementType type,
                                BigDecimal qty, String reason, String refType, Long refId,
                                String by, BigDecimal after) {
        return movements.save(builder
                .movementType(type)
                .qty(qty)
                .reason(reason)
                .refType(refType)
                .refId(refId)
                .balanceAfter(after)
                .createdBy(by)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * The quantity is how much moved, never which way — the type carries that.
     * A negative here would subtract from an OUT and silently add stock.
     */
    private void require(BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("A movement quantity must be greater than zero");
        }
    }

    private void warnIfNegative(String what, BigDecimal after) {
        if (after.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Stock for '{}' is now negative ({})", what, after);
        }
    }
}
