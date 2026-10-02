package com.resturant.management.rms.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

    /**
     * For the scanner. ARCHITECTURE §1.3: a barcode is a lookup, not a search,
     * and most dishes have none — so this answers for the packaged things
     * that do.
     */
    Optional<ProductVariant> findFirstByBarcodeAndActYn(String barcode, String actYn);
}
