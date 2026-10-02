import type { PaymentMethod } from "@/types/order";

/**
 * What is still returnable on one line of a settled bill.
 *
 * <p>The server computes `remainingQty`; the client never subtracts for
 * itself. No database constraint can stop a line of two being returned three
 * times across two documents, so the subtraction happens where the write
 * happens — and happens again when the return is posted, because a number that
 * travelled to a browser and back is a suggestion.
 */
export interface ReturnableLine {
  orderItemId: number;
  productId?: number;
  productName: string;
  soldQty: number;
  returnedQty: number;
  remainingQty: number;
  unitPrice: number;
}

export interface ReturnableOrder {
  orderId: number;
  invoiceNo: string;
  paidAt?: string;
  total: number;
  /** What the bill was settled by. A refund defaults to the same. */
  originalMethod?: PaymentMethod;
  anythingLeft: boolean;
  lines: ReturnableLine[];
}

export interface ReturnLineRequest {
  orderItemId: number;
  qty: number;
}

export interface CreateReturnRequest {
  orderId: number;
  lines: ReturnLineRequest[];
  reason: string;
  /** Omitted means "however it was paid". */
  refundMethod?: PaymentMethod;
}

export interface ReturnItem {
  id: number;
  orderItemId: number;
  productName: string;
  qty: number;
  unitPrice: number;
  lineTotal: number;
}

export interface SaleReturn {
  id: number;
  returnNo: string;
  orderId: number;
  invoiceNo: string;
  total: number;
  refundMethod: PaymentMethod;
  reason: string;
  createdBy?: string;
  createdAt: string;
  /** Present only when the amount meant somebody had to sign for it. */
  approvedBy?: string;
  shiftId?: number;
  items: ReturnItem[];
}
