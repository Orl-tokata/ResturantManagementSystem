import type { OrderStatus, PaymentMethod } from "@/types/order";

/**
 * Why a bill cannot be settled yet, as a key in the `payment` namespace.
 *
 * <p>Null means it can. Returning a key rather than a sentence keeps this
 * function free of translation, which is what makes it testable.
 */
export type PayBlocker =
  | "alreadyPaid"
  | "alreadyCancelled"
  | "awaitingKhqr"
  | "noItems"
  | "enterTendered"
  | "shortfall"
  | null;

export interface TenderInput {
  /** The server's total. Never recomputed here. */
  total: number;
  /** Exactly what is in the box, including a lone "." mid-typing. */
  tendered: string;
  method: PaymentMethod;
  status: OrderStatus | undefined;
  itemCount: number;
}

export interface TenderState {
  amount: number;
  change: number;
  shortfall: boolean;
  canPay: boolean;
  blocker: PayBlocker;
}

/**
 * Parses the tender box.
 *
 * <p>`Number("")` is 0 but `Number(".")` is NaN, and the keypad can produce a
 * lone decimal point as the first press. NaN then poisons the change, the
 * comparison and the message — so it is treated as nothing typed yet, which is
 * what it is.
 */
export function parseTendered(tendered: string): number {
  const n = Number(tendered);
  return Number.isFinite(n) ? n : 0;
}

/**
 * Everything the settle button and its explanation need, from one place.
 *
 * <p>`canPay` and `blocker` are derived together on purpose. They used to be
 * two expressions listing the same conditions in the same order, which is a
 * standing invitation for a disabled button with no reason given, or a reason
 * given beside an enabled one.
 */
export function tenderState(input: TenderInput): TenderState {
  const { total, tendered, method, status, itemCount } = input;

  const amount = parseTendered(tendered);
  const change = amount - total;
  const isCash = method === "CASH";
  const typed = tendered !== "" && tendered !== ".";
  const shortfall = isCash && typed && change < 0;

  const blocker = blockerFor({ status, itemCount, isCash, typed, change });

  return {
    amount,
    change,
    shortfall,
    // A missing order blocks too, but says nothing — the screen is still
    // loading and has no news for the cashier.
    canPay: status !== undefined && blocker === null,
    blocker,
  };
}

function blockerFor(o: {
  status: OrderStatus | undefined;
  itemCount: number;
  isCash: boolean;
  typed: boolean;
  change: number;
}): PayBlocker {
  if (o.status === "PAID") return "alreadyPaid";
  if (o.status === "CANCELLED") return "alreadyCancelled";
  // A KHQR code is live and the bill is frozen until it is paid or abandoned.
  // Before AWAITING_PAYMENT was handled here it fell through to the
  // already-cancelled branch, so a cashier watching a customer scan was told
  // the bill had been cancelled.
  if (o.status === "AWAITING_PAYMENT") return "awaitingKhqr";
  if (o.itemCount === 0) return "noItems";
  if (o.isCash && !o.typed) return "enterTendered";
  if (o.isCash && o.change < 0) return "shortfall";
  return null;
}
