/**
 * The till session a cashier works inside.
 *
 * <p>Optional fields are optional rather than `| null` because the server omits
 * null columns: an open shift has not been counted, so it carries no declared
 * cash and no variance at all.
 */
export type ShiftStatus = "OPEN" | "CLOSED";

export type CashMovementType =
  | "SALE"
  | "REFUND"
  | "PAY_IN"
  | "PAY_OUT"
  | "FLOAT"
  | "DROP";

/** The four a person may record. SALE and REFUND follow from bills. */
export const MANUAL_MOVEMENTS: CashMovementType[] = ["PAY_IN", "PAY_OUT", "FLOAT", "DROP"];

export interface MovementTotal {
  type: CashMovementType;
  total: number;
  count: number;
}

export interface Shift {
  id: number;
  userId: number;
  userName: string;
  status: ShiftStatus;
  openedAt: string;
  closedAt?: string;
  openingFloat: number;
  /** Float plus every movement; the stored figure once the shift is closed. */
  expectedCash: number;
  declaredCash?: number;
  /** Declared minus expected. Negative is short, positive is over. */
  variance?: number;
  note?: string;
  totals: MovementTotal[];
  cashSales: number;
  saleCount: number;
}

export interface CashMovement {
  id: number;
  type: CashMovementType;
  amount: number;
  /** True when this put money in; `amount` itself is always positive. */
  increase: boolean;
  reason?: string;
  refType?: string;
  refId?: number;
  createdBy: string;
  createdAt: string;
}

export interface ShiftDetail {
  shift: Shift;
  movements: CashMovement[];
}
