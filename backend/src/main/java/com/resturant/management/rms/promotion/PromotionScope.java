package com.resturant.management.rms.promotion;

/** What a rule applies to. */
public enum PromotionScope {
    /** One dish. {@code scopeId} is its product id. */
    ITEM,
    /** Everything in a category. {@code scopeId} is the category id. */
    CATEGORY,
    /** The bill as a whole. {@code scopeId} is null. */
    ORDER
}
