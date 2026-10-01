import type { RecordStatus } from "@/types/master";

/* ---- Supplier ------------------------------------------------------------ */

export interface Supplier {
  id: number;
  supplierCode: string;
  company: string;
  contactPerson: string | null;
  phone: string | null;
  email: string | null;
  supplyType: string | null;
  address: string | null;
  balance: number;
  status: RecordStatus;
}

export interface SupplierRequest {
  supplierCode: string;
  company: string;
  contactPerson?: string;
  phone?: string;
  email?: string;
  supplyType?: string;
  address?: string;
  status?: RecordStatus;
}

export const SUPPLY_TYPES = [
  "MEAT",
  "VEGETABLE",
  "SEAFOOD",
  "DRINK",
  "RICE",
  "OTHER",
] as const;

/* ---- Stock --------------------------------------------------------------- */

/** Mirrors the backend enum. SALE and RETURN arrived with the ledger in V11. */
export type MovementType = "IN" | "OUT" | "DAMAGED" | "SALE" | "RETURN";

export interface StockItem {
  id: number;
  name: string;
  unit: string;
  qty: number;
  minQty: number;
  unitCost: number;
  value: number;
  lowStock: boolean;
  outOfStock: boolean;
}

export interface StockItemRequest {
  name: string;
  unit: string;
  qty?: number;
  minQty?: number;
  unitCost?: number;
}

export interface AdjustRequest {
  /** A correction is IN, OUT or DAMAGED; SALE and RETURN are written by the server. */
  type: MovementType;
  qty: number;
  reason?: string;
}

/**
 * One line of the stock ledger.
 *
 * <p>The optional fields are optional rather than `| null` because the server
 * serialises with `non_null`: a null column is left out of the JSON entirely
 * and arrives here as `undefined`. Declaring them `| null` reads as honest and
 * is not — it invites `=== null`, which is false for every one of them.
 */
export interface Movement {
  id: number;
  /** A movement names a product or a stock item, never both. */
  stockItemId?: number;
  stockItemName?: string;
  productId?: number;
  productName?: string;
  type: MovementType;
  qty: number;
  /** True when this added stock; `qty` itself is always positive. */
  increase: boolean;
  /**
   * The balance immediately after this movement. Absent on rows written before
   * the ledger existed — the balance before them was never recorded, so a
   * figure here would be a guess reading as a record.
   */
  balanceAfter?: number;
  /** What caused it, e.g. ORDER or PURCHASE, and that document's id. */
  refType?: string;
  refId?: number;
  reason?: string;
  createdBy?: string;
  createdAt: string;
}

export interface StockSummary {
  totalItems: number;
  stockValue: number;
  lowStockCount: number;
  outOfStockCount: number;
}

/* ---- Purchase ------------------------------------------------------------ */

export type PurchaseStatus = "PENDING" | "RECEIVED" | "CANCELLED";

export interface PurchaseItem {
  id: number | null;
  stockItemId: number | null;
  itemName: string;
  qty: number;
  unitCost: number;
  lineTotal: number;
}

export interface Purchase {
  id: number;
  poNo: string;
  supplierId: number | null;
  supplierName: string | null;
  purchaseDate: string;
  items: PurchaseItem[];
  total: number;
  status: PurchaseStatus;
  note: string | null;
}

/** One line of the purchase-order form, before it is sent. */
export interface PurchaseDraftLine {
  stockItemId: number;
  itemName: string;
  unit: string;
  qty: number;
  unitCost: number;
}

export interface PurchaseSummary {
  monthTotal: number;
  orderCount: number;
  pendingCount: number;
  payable: number;
}
