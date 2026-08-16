package com.resturant.management.rms.stock;

public enum MovementType {
    /** Received from a purchase, or a manual increase. */
    IN,
    /** Consumed by a sale, or a manual decrease. */
    OUT,
    /** Spoiled, broken or lost. */
    DAMAGED
}
