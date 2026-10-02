package com.resturant.management.rms.catalog;

import com.resturant.management.rms.catalog.dto.CatalogDtos.ProductRequest;
import com.resturant.management.rms.catalog.dto.CatalogDtos.ProductResponse;
import com.resturant.management.rms.common.RecordStatus;
import com.resturant.management.rms.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static com.resturant.management.rms.common.Strings.blankToNull;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final com.resturant.management.rms.storage.ImageStorageService imageStorage;

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String query, Long categoryId, Pageable pageable) {
        return productRepository.search(blankToNull(query), categoryId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        apply(product, request);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        apply(product, request);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        // OrderItem keeps a nullable product_id plus its own copy of the name and
        // price, so removing a product never damages historical receipts.
        Product product = find(id);
        String image = product.getImageFile();
        productRepository.delete(product);
        // After the row, not before: a delete that fails on a foreign key would
        // otherwise leave a product pointing at a file that no longer exists.
        imageStorage.delete(image);
    }

    /** Replaces a product's photograph, discarding whatever it had before. */
    @Transactional
    public ProductResponse setImage(Long id, org.springframework.web.multipart.MultipartFile file) {
        Product product = find(id);
        String previous = product.getImageFile();
        product.setImageFile(imageStorage.store(file));
        ProductResponse saved = toResponse(productRepository.save(product));
        imageStorage.delete(previous);
        return saved;
    }

    /** Removes a product's photograph. The icon takes over on the screens. */
    @Transactional
    public ProductResponse clearImage(Long id) {
        Product product = find(id);
        String previous = product.getImageFile();
        product.setImageFile(null);
        ProductResponse saved = toResponse(productRepository.save(product));
        imageStorage.delete(previous);
        return saved;
    }

    /* ---- Helpers --------------------------------------------------------- */

    private Product find(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.product", id));
    }

    private void apply(Product product, ProductRequest r) {
        Category category = categoryRepository.findById(r.categoryId())
                .orElseThrow(() -> NotFoundException.of("entity.category", r.categoryId()));

        product.setName(r.name());
        product.setNameEn(r.nameEn());
        product.setCategory(category);
        product.setPrice(r.price());
        product.setCost(r.cost() != null ? r.cost() : BigDecimal.ZERO);
        product.setStockQty(r.stockQty() != null ? r.stockQty() : BigDecimal.ZERO);
        product.setIcon(r.icon());
        product.setDescription(r.description());
        product.setStatus(r.status() != null ? r.status() : RecordStatus.ACTIVE);
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(), p.getName(), p.getNameEn(),
                p.getCategory().getId(), p.getCategory().getName(),
                p.getPrice(), p.getCost(), p.getStockQty(),
                p.getIcon(), p.getImageFile(), p.getDescription(), p.getStatus(),
                !p.getVariants().isEmpty() || !p.getModifierGroups().isEmpty());
    }
}
