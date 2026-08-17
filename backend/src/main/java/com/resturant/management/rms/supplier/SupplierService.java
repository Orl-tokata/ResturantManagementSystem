package com.resturant.management.rms.supplier;

import com.resturant.management.rms.common.ConflictHelper;
import com.resturant.management.rms.common.RecordStatus;
import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.NotFoundException;
import com.resturant.management.rms.purchase.PurchaseRepository;
import com.resturant.management.rms.supplier.dto.SupplierDtos.SupplierRequest;
import com.resturant.management.rms.supplier.dto.SupplierDtos.SupplierResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.resturant.management.rms.common.Strings.blankToNull;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;

    @Transactional(readOnly = true)
    public Page<SupplierResponse> search(String query, Pageable pageable) {
        return supplierRepository.search(blankToNull(query), pageable).map(this::toResponse);
    }

    /** Un-paged and active only — feeds the supplier dropdown on a purchase order. */
    @Transactional(readOnly = true)
    public List<SupplierResponse> listActive() {
        return supplierRepository.findByStatus(RecordStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        if (supplierRepository.existsBySupplierCode(request.supplierCode())) {
            throw ConflictHelper.duplicate("Supplier", "code", request.supplierCode());
        }
        Supplier supplier = new Supplier();
        apply(supplier, request);
        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = find(id);

        supplierRepository.findBySupplierCode(request.supplierCode())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw ConflictHelper.duplicate("Supplier", "code", request.supplierCode());
                });

        apply(supplier, request);
        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional
    public void delete(Long id) {
        Supplier supplier = find(id);

        long orders = purchaseRepository.countBySupplierId(id);
        if (orders > 0) {
            throw ConflictHelper.inUse("Supplier", supplier.getCompany(), orders, "purchase order(s)");
        }
        // A non-zero balance means money is still owed; removing the record would
        // lose that debt silently.
        if (supplier.getBalance() != null && supplier.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BadRequestException(
                    "Cannot delete '%s': %s is still outstanding. Settle the balance first."
                            .formatted(supplier.getCompany(), supplier.getBalance()));
        }
        supplierRepository.delete(supplier);
    }

    /* ---- Helpers --------------------------------------------------------- */

    private Supplier find(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Supplier", id));
    }

    private void apply(Supplier s, SupplierRequest r) {
        s.setSupplierCode(r.supplierCode());
        s.setCompany(r.company());
        s.setContactPerson(r.contactPerson());
        s.setPhone(r.phone());
        s.setEmail(r.email());
        s.setSupplyType(r.supplyType());
        s.setAddress(r.address());
        s.setStatus(r.status() != null ? r.status() : RecordStatus.ACTIVE);
        // balance is never set from a request — it moves only when goods are
        // received or a payment is recorded.
    }

    private SupplierResponse toResponse(Supplier s) {
        return new SupplierResponse(
                s.getId(), s.getSupplierCode(), s.getCompany(), s.getContactPerson(),
                s.getPhone(), s.getEmail(), s.getSupplyType(), s.getAddress(),
                s.getBalance(), s.getStatus());
    }
}
