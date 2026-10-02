/**
 * A customer and their points.
 *
 * <p>Optional fields are optional rather than `| null` because the server omits
 * null columns — a customer who has never been in has no last visit at all.
 *
 * <p>`points` is summed from the ledger on every read. There is no balance
 * column to drift from the rows behind it.
 */
export interface Customer {
  id: number;
  code: string;
  name: string;
  phone?: string;
  email?: string;
  birthDate?: string;
  note?: string;
  points: number;
  totalSpent: number;
  visitCount: number;
  lastVisit?: string;
}

export interface CustomerRequest {
  name: string;
  phone?: string;
  email?: string;
  birthDate?: string;
  note?: string;
}

export type LoyaltyType = "EARN" | "REDEEM" | "ADJUST" | "EXPIRE" | "REVERSE";

export interface LoyaltyEntry {
  id: number;
  type: LoyaltyType;
  /** Signed: positive adds, negative takes away. */
  points: number;
  orderId?: number;
  invoiceNo?: string;
  note?: string;
  createdBy?: string;
  createdAt: string;
}

export interface LoyaltyAdjustRequest {
  points: number;
  note: string;
}
