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

export type MovementType = "IN" | "OUT" | "DAMAGED";

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
  type: MovementType;
  qty: number;
  reason?: string;
}

export interface Movement {
  id: number;
  stockItemId: number | null;
  stockItemName: string | null;
  type: MovementType;
  qty: number;
  reason: string | null;
  createdBy: string | null;
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
