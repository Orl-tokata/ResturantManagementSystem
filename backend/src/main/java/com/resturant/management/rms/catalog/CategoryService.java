package com.resturant.management.rms.catalog;

import com.resturant.management.rms.catalog.dto.CatalogDtos.CategoryRequest;
import com.resturant.management.rms.catalog.dto.CatalogDtos.CategoryResponse;
import com.resturant.management.rms.common.ConflictHelper;
import static com.resturant.management.rms.common.Strings.blankToNull;
import com.resturant.management.rms.common.RecordStatus;
import com.resturant.management.rms.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<CategoryResponse> search(String query, Pageable pageable) {
        return categoryRepository.search(blankToNull(query), pageable).map(this::toResponse);
    }

    /** Un-paged, active only — feeds the POS category rail and every category dropdown. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return categoryRepository.findByStatusOrderBySortOrderAsc(RecordStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        apply(category, request);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = find(id);
        apply(category, request);
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = find(id);

        // Checked up front so the user gets a useful message instead of a raw
        // foreign-key violation from the database.
        long inUse = productRepository.countByCategoryId(id);
        if (inUse > 0) {
            throw ConflictHelper.inUse("entity.category", category.getName(), inUse, "usedBy.products");
        }
        categoryRepository.delete(category);
    }

    /* ---- Helpers --------------------------------------------------------- */

    private Category find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.category", id));
    }

    private void apply(Category category, CategoryRequest r) {
        category.setName(r.name());
        category.setNameEn(r.nameEn());
        category.setIcon(r.icon());
        category.setSortOrder(r.sortOrder() != null ? r.sortOrder() : 0);
        category.setStatus(r.status() != null ? r.status() : RecordStatus.ACTIVE);
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(
                c.getId(), c.getName(), c.getNameEn(), c.getIcon(),
                c.getSortOrder(), c.getStatus(),
                productRepository.countByCategoryId(c.getId()));
    }
}
