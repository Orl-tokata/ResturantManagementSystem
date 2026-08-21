export type OrderStatus = "OPEN" | "PAID" | "CANCELLED";
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

export interface Order {
  id: number;
  invoiceNo: string;
  tableId: number | null;
  tableName: string | null;
  cashierId: number | null;
  cashierName: string | null;
  guestCount: number | null;
  items: OrderItem[];
  subtotal: number;
  discount: number;
  vatRate: number;
  vatAmount: number;
  total: number;
  totalKhr: number;
  paymentMethod: PaymentMethod | null;
  amountTendered: number | null;
  changeAmount: number | null;
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
