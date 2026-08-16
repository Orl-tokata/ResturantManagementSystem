package com.resturant.management.rms.purchase;

public enum PurchaseStatus {
    /** Raised but goods not yet received — stock is untouched. */
    PENDING,
    /** Goods received — stock has been incremented. */
    RECEIVED,
    CANCELLED
}
