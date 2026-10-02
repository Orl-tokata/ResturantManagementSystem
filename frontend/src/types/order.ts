/**
 * Mirrors the backend enum. AWAITING_PAYMENT means a KHQR code is on screen
 * and the till is waiting for Bakong — the bill is frozen, not settled and not
 * cancelled. It was missing here, so screens comparing against "CANCELLED" as
 * the only other possibility told cashiers a live bill had been cancelled.
 */
export type OrderStatus = "OPEN" | "AWAITING_PAYMENT" | "PAID" | "CANCELLED";
export type PaymentMethod = "CASH" | "CARD" | "KHQR" | "TRANSFER";

export interface OrderItem {
  id: number | null;
  productId: number | null;
  productName: string;
  qty: number;
  unitPrice: number;
  lineTotal: number;
  note: string | null;
}

export type PaymentStatus = "PENDING" | "CAPTURED" | "FAILED" | "REFUNDED";

/**
 * One payment against a bill.
 *
 * <p>A bill has a list of these rather than a method and a tender, so it can
 * be settled by more than one method and so a QR code that was shown and never
 * paid still leaves a row — FAILED, not absent.
 *
 * <p>The optional fields are optional, not `| null`: the server omits null
 * columns, so they arrive as `undefined`. Only cash carries a tender.
 */
export interface Payment {
  id: number;
  method: PaymentMethod;
  amount: number;
  amountKhr?: number;
  tendered?: number;
  changeAmount?: number;
  reference?: string;
  status: PaymentStatus;
  createdAt: string;
}

export interface Order {
  id: number;
  invoiceNo: string;
  tableId: number | null;
  tableName: string | null;
  cashierId: number | null;
  cashierName: string | null;
  /** Absent on a walk-in, which is most bills. */
  customerId?: number;
  customerName?: string;
  guestCount: number | null;
  items: OrderItem[];
  subtotal: number;
  discount: number;
  vatRate: number;
  vatAmount: number;
  total: number;
  totalKhr: number;
  /** The rate totalKhr was worked out at, stamped when the bill was. */
  fxRateKhr?: number;
  /** Every attempt, settled or not, oldest first. */
  payments: Payment[];
  /**
   * Derived by the server from the captured payments, and absent rather than
   * null when they do not answer the question: no method on a split bill,
   * because naming one of two would be a guess, and no tender on a card.
   */
  paymentMethod?: PaymentMethod;
  amountTendered?: number;
  changeAmount?: number;
  status: OrderStatus;
  createdAt: string | null;
  paidAt: string | null;
}

/** One line of the local basket, before it is sent to the server. */
export interface CartLine {
  productId: number;
  productName: string;
  unitPrice: number;
  qty: number;
  note?: string;
}

/** Every method, in the order a till offers them. */
export const PAYMENT_METHODS: PaymentMethod[] = ["CASH", "CARD", "KHQR", "TRANSFER"];

export const PAYMENT_ICON: Record<PaymentMethod, string> = {
  CASH: "💵",
  CARD: "💳",
  KHQR: "📱",
  TRANSFER: "🏦",
};

/** Response of {@code GET /api/orders/{id}/receipt}. */
export interface Receipt {
  restaurantName: string;
  restaurantNameEn: string;
  address: string;
  phone: string;
  order: Order;
}
