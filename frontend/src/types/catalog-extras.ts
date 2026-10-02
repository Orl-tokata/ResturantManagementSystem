/**
 * Sizes and the things people ask for.
 *
 * <p>A **variant** is a distinct sellable thing with its own price — a large
 * coffee beside a small one. A **modifier** is a property of one line: no ice,
 * extra spicy, takeaway box. ARCHITECTURE §1.2 keeps them apart because
 * expressing the second as the first multiplies the menu for something nobody
 * stocks.
 */
export interface Variant {
  id: number;
  productId: number;
  name: string;
  nameEn?: string;
  /** What this size sells for. Not a delta — a size is priced, not adjusted. */
  price: number;
  cost?: number;
  sku?: string;
  barcode?: string;
  sortOrder: number;
}

export interface VariantRequest {
  name: string;
  nameEn?: string;
  price: number;
  cost?: number;
  sku?: string;
  barcode?: string;
  sortOrder?: number;
}

export interface Modifier {
  id: number;
  name: string;
  nameEn?: string;
  /** Signed: free, dearer, or a reduction for leaving something out. */
  priceDelta: number;
  sortOrder: number;
}

export interface ModifierGroup {
  id: number;
  name: string;
  nameEn?: string;
  /** 0..1 is "anything else?", 1..1 is "choose one", 0..n is "tick what you want". */
  minSelect: number;
  maxSelect: number;
  sortOrder: number;
  required: boolean;
  modifiers: Modifier[];
}

export interface ModifierGroupRequest {
  name: string;
  nameEn?: string;
  minSelect: number;
  maxSelect: number;
  sortOrder?: number;
  modifiers: {
    name: string;
    nameEn?: string;
    priceDelta: number;
    sortOrder?: number;
  }[];
}

/** What the chooser needs to ask about one dish. */
export interface ProductOptions {
  variants: Variant[];
  groups: ModifierGroup[];
}
