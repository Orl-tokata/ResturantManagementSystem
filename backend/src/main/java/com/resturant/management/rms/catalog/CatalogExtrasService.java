package com.resturant.management.rms.catalog;

import com.resturant.management.rms.catalog.dto.CatalogDtos.*;
import com.resturant.management.rms.common.Strings;
import com.resturant.management.rms.common.exception.BadRequestException;
import com.resturant.management.rms.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sizes and questions.
 *
 * <p>Separate from {@link ProductService} because it is a separate job — a
 * product's own fields are edited on one screen and its sizes on the same one,
 * but modifier groups are shared across the menu and belong to nobody.
 *
 * <p>Nothing here takes a branch. Variants hang off a product, and the product
 * is loaded through the branch filter, so asking for one that belongs to
 * another shop fails at the first step. Modifier groups are deliberately
 * unscoped: one company's shops share a menu's structure (V17).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogExtrasService {

    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ModifierGroupRepository groups;

    /* ===================================================================== */
    /* Variants                                                              */
    /* ===================================================================== */

    @Transactional(readOnly = true)
    public List<VariantResponse> variantsOf(Long productId) {
        product(productId);
        return variants.findByProductIdOrderBySortOrderAscIdAsc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public VariantResponse addVariant(Long productId, VariantRequest request) {
        Product product = product(productId);

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .name(request.name().trim())
                .nameEn(Strings.blankToNull(request.nameEn()))
                .price(request.price())
                .cost(request.cost() == null ? BigDecimal.ZERO : request.cost())
                .sku(Strings.blankToNull(request.sku()))
                .barcode(Strings.blankToNull(request.barcode()))
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .build();

        return toResponse(variants.save(variant));
    }

    @Transactional
    public VariantResponse updateVariant(Long productId, Long variantId, VariantRequest request) {
        ProductVariant variant = variantOf(productId, variantId);

        variant.setName(request.name().trim());
        variant.setNameEn(Strings.blankToNull(request.nameEn()));
        variant.setPrice(request.price());
        variant.setCost(request.cost() == null ? BigDecimal.ZERO : request.cost());
        variant.setSku(Strings.blankToNull(request.sku()));
        variant.setBarcode(Strings.blankToNull(request.barcode()));
        variant.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());

        return toResponse(variants.save(variant));
    }

    /**
     * Removes a size.
     *
     * <p>No check for sold lines: {@code order_item.variant_id} is nullable
     * and the line carries the name it was sold under, so history survives the
     * row going away. That is the same arrangement {@code modifier_id} has,
     * and the reason both are nullable.
     */
    @Transactional
    public void deleteVariant(Long productId, Long variantId) {
        variants.delete(variantOf(productId, variantId));
    }

    /* ===================================================================== */
    /* Modifier groups                                                       */
    /* ===================================================================== */

    @Transactional(readOnly = true)
    public List<ModifierGroupResponse> allGroups() {
        return groups.findByActYnOrderBySortOrderAscIdAsc("Y").stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ModifierGroupResponse createGroup(ModifierGroupRequest request) {
        ModifierGroup group = new ModifierGroup();
        apply(group, request);
        return toResponse(groups.save(group));
    }

    @Transactional
    public ModifierGroupResponse updateGroup(Long id, ModifierGroupRequest request) {
        ModifierGroup group = group(id);
        // Replaced wholesale rather than matched up by id: the options are the
        // question, and the screen sends the whole question back.
        group.getModifiers().clear();
        apply(group, request);
        return toResponse(groups.save(group));
    }

    @Transactional
    public void deleteGroup(Long id) {
        groups.delete(group(id));
    }

    /* ===================================================================== */
    /* Attaching                                                             */
    /* ===================================================================== */

    @Transactional
    public List<ModifierGroupResponse> attach(Long productId, Long groupId) {
        Product product = product(productId);
        ModifierGroup group = group(groupId);

        if (product.getModifierGroups().stream().noneMatch(g -> g.getId().equals(groupId))) {
            product.getModifierGroups().add(group);
            products.save(product);
        }
        return product.getModifierGroups().stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<ModifierGroupResponse> detach(Long productId, Long groupId) {
        Product product = product(productId);
        product.getModifierGroups().removeIf(g -> g.getId().equals(groupId));
        products.save(product);
        return product.getModifierGroups().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ModifierGroupResponse> groupsOf(Long productId) {
        return product(productId).getModifierGroups().stream().map(this::toResponse).toList();
    }

    /* ===================================================================== */
    /* Internals                                                             */
    /* ===================================================================== */

    private void apply(ModifierGroup group, ModifierGroupRequest request) {
        int min = request.minSelect() == null ? 0 : request.minSelect();
        int max = request.maxSelect() == null ? 1 : request.maxSelect();
        if (max < min) {
            throw new BadRequestException("error.modifier.selectRange");
        }
        if (max > request.modifiers().size()) {
            // Asking for three of two options is a question nobody can answer.
            throw new BadRequestException("error.modifier.maxAboveOptions",
                    max, request.modifiers().size());
        }

        group.setName(request.name().trim());
        group.setNameEn(Strings.blankToNull(request.nameEn()));
        group.setMinSelect(min);
        group.setMaxSelect(max);
        group.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());

        for (ModifierRequest m : request.modifiers()) {
            group.addModifier(Modifier.builder()
                    .name(m.name().trim())
                    .nameEn(Strings.blankToNull(m.nameEn()))
                    .priceDelta(m.priceDelta() == null ? BigDecimal.ZERO : m.priceDelta())
                    .sortOrder(m.sortOrder() == null ? 0 : m.sortOrder())
                    .build());
        }
    }

    private Product product(Long id) {
        return products.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.product", id));
    }

    private ModifierGroup group(Long id) {
        return groups.findById(id)
                .orElseThrow(() -> NotFoundException.of("entity.modifierGroup", id));
    }

    /** A variant is only reachable through the product it belongs to. */
    private ProductVariant variantOf(Long productId, Long variantId) {
        product(productId);
        ProductVariant variant = variants.findById(variantId)
                .orElseThrow(() -> NotFoundException.of("entity.variant", variantId));
        if (!variant.getProduct().getId().equals(productId)) {
            throw NotFoundException.of("entity.variant", variantId);
        }
        return variant;
    }

    private VariantResponse toResponse(ProductVariant v) {
        return new VariantResponse(v.getId(), v.getProduct().getId(), v.getName(), v.getNameEn(),
                v.getPrice(), v.getCost(), v.getSku(), v.getBarcode(), v.getSortOrder());
    }

    private ModifierGroupResponse toResponse(ModifierGroup g) {
        return new ModifierGroupResponse(
                g.getId(), g.getName(), g.getNameEn(),
                g.getMinSelect(), g.getMaxSelect(), g.getSortOrder(), g.isRequired(),
                g.getModifiers().stream()
                        .map(m -> new ModifierResponse(m.getId(), m.getName(), m.getNameEn(),
                                m.getPriceDelta(), m.getSortOrder()))
                        .toList());
    }
}
