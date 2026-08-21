package com.resturant.management.rms.purchase;

import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.purchase.dto.PurchaseDtos.*;
import com.resturant.management.rms.setting.SettingService;
import com.resturant.management.rms.stock.StockItem;
import com.resturant.management.rms.stock.StockService;
import com.resturant.management.rms.supplier.Supplier;
import com.resturant.management.rms.supplier.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static com.resturant.management.rms.common.Strings.blankToNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private static final int MONEY_SCALE = 2;

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final StockService stockService;
    private final SettingService settings;

    /* ---- Reads ----------------------------------------------------------- */

    @Transactional(readOnly = true)
    public Page<PurchaseResponse> search(String query, PurchaseStatus status, Pageable pageable) {
        return purchaseRepository.search(blankToNull(query), status, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PurchaseResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public PurchaseSummary summary(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now();

        BigDecimal monthTotal = purchaseRepository.sumTotalBetween(start, end);
        long orders = purchaseRepository.countByPurchaseDateBetween(start, end);
        long pending = purchaseRepository.countByStatus(PurchaseStatus.PENDING);
        BigDecimal payable = supplierRepository.sumBalances();

        return new PurchaseSummary(monthTotal, orders, pending, payable);
    }

    /* ---- Writes ---------------------------------------------------------- */

    /**
     * Raises a purchase order in PENDING. Stock is untouched until the goods are
     * actually received — see {@link #receive}.
     */
    @Transactional
    public PurchaseResponse create(PurchaseRequest request) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> NotFoundException.of("entity.supplier", request.supplierId()));

        Purchase purchase = Purchase.builder()
                .poNo(nextPoNo())
                .supplier(supplier)
                .purchaseDate(request.purchaseDate())
                .status(PurchaseStatus.PENDING)
                .note(request.note())
                .build();

        for (PurchaseItemRequest line : request.items()) {
            StockItem stockItem = stockService.find(line.stockItemId());
            PurchaseItem item = PurchaseItem.builder()
                    .stockItem(stockItem)
                    // Copied, so renaming an ingredient later cannot rewrite this order.
                    .itemName(stockItem.getName())
                    .qty(line.qty())
                    .unitCost(line.unitCost())
                    .lineTotal(line.qty().multiply(line.unitCost())
                            .setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                    .build();
            purchase.addItem(item);
        }

        recalculate(purchase);
        Purchase saved = purchaseRepository.save(purchase);
        log.info("Raised {} on {} for {}",
                saved.getPoNo(), saved.getSupplier().getCompany(), saved.getTotal());
        return toResponse(saved);
    }

    /**
     * Marks the order received: every line increases its stock item, a movement
     * is written for each, and the supplier's payable goes up.
     *
     * <p>One transaction — a partial receipt would leave stock and the ledger
     * disagreeing with each other.
     */
    @Transactional
    public PurchaseResponse receive(Long id, String username) {
        Purchase purchase = find(id);

        if (purchase.getStatus() == PurchaseStatus.RECEIVED) {
            throw new BadRequestException("error.purchase.alreadyReceived", purchase.getPoNo());
        }
        if (purchase.getStatus() == PurchaseStatus.CANCELLED) {
            throw new BadRequestException("error.purchase.cancelled", purchase.getPoNo());
        }

        for (PurchaseItem item : purchase.getItems()) {
            stockService.receive(item.getStockItem(), item.getQty(), purchase.getPoNo(), username);

            // Keep the unit cost current so stock valuation reflects what was
            // last actually paid, not the price when the item was created.
            item.getStockItem().setUnitCost(item.getUnitCost());
        }

        purchase.setStatus(PurchaseStatus.RECEIVED);

        Supplier supplier = purchase.getSupplier();
        BigDecimal balance = supplier.getBalance() == null ? BigDecimal.ZERO : supplier.getBalance();
        supplier.setBalance(balance.add(purchase.getTotal()));
        supplierRepository.save(supplier);

        log.info("Received {} — {} lines, {} added to {} payable",
                purchase.getPoNo(), purchase.getItems().size(),
                purchase.getTotal(), supplier.getCompany());
        return toResponse(purchaseRepository.save(purchase));
    }

    @Transactional
    public PurchaseResponse cancel(Long id) {
        Purchase purchase = find(id);
        if (purchase.getStatus() == PurchaseStatus.RECEIVED) {
            throw new BadRequestException("error.purchase.receivedCancel", purchase.getPoNo());
        }
        purchase.setStatus(PurchaseStatus.CANCELLED);
        return toResponse(purchaseRepository.save(purchase));
    }

    @Transactional
    public void delete(Long id) {
        Purchase purchase = find(id);
        if (purchase.getStatus() == PurchaseStatus.RECEIVED) {
            throw new BadRequestException("error.purchase.receivedDelete", purchase.getPoNo());
        }
        purchaseRepository.delete(purchase);
    }

    /* ---- Helpers ---------------------------------------------------------- */

    private Purchase find(Long id) {
        return purchaseRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.purchase", id));
    }

    private void recalculate(Purchase purchase) {
        BigDecimal total = purchase.getItems().stream()
                .map(PurchaseItem::getLineTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        purchase.setTotal(total);
    }

    private String nextPoNo() {
        Long seq = purchaseRepository.nextPurchaseSequence();
        return "%s%05d".formatted(settings.purchasePrefix(), seq);
    }

    private PurchaseResponse toResponse(Purchase p) {
        List<PurchaseItemResponse> items = p.getItems().stream()
                .map(i -> new PurchaseItemResponse(
                        i.getId(),
                        i.getStockItem() != null ? i.getStockItem().getId() : null,
                        i.getItemName(), i.getQty(), i.getUnitCost(), i.getLineTotal()))
                .toList();

        return new PurchaseResponse(
                p.getId(), p.getPoNo(),
                p.getSupplier() != null ? p.getSupplier().getId() : null,
                p.getSupplier() != null ? p.getSupplier().getCompany() : null,
                p.getPurchaseDate(), items, p.getTotal(), p.getStatus(), p.getNote());
    }
}
