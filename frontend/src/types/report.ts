/** Takings in one hour of the day, summed across the report's range. */
export interface HourlyPoint {
  /** 0–23, local time. */
  hour: number;
  total: number;
  orders: number;
}

export interface DailyPoint {
  date: string;
  total: number;
  orders: number;
}

export interface BestSeller {
  productName: string;
  qty: number;
  revenue: number;
}

export interface LowStockRow {
  id: number;
  name: string;
  unit: string;
  qty: number;
  minQty: number;
}

export interface CategoryRevenue {
  name: string;
  nameEn: string | null;
  qty: number;
  revenue: number;
  percent: number;
}

export interface DashboardSummary {
  todaySales: number;
  monthSales: number;
  paidInvoices: number;
  lowStockCount: number;
  tablesFree: number;
  tablesOccupied: number;
  tablesReserved: number;
  lastSevenDays: DailyPoint[];
  bestSellers: BestSeller[];
  lowStock: LowStockRow[];
}

export interface CashierSummary {
  todaySales: number;
  todayInvoices: number;
  tablesOccupied: number;
  tablesTotal: number;
  openBills: number;
}

export interface SalesReport {
  from: string;
  to: string;
  revenue: number;
  cost: number;
  grossProfit: number;
  marginPercent: number;
  invoiceCount: number;
  averageSale: number;
  daily: DailyPoint[];
  hourly: HourlyPoint[];
  byCategory: CategoryRevenue[];
  bestSellers: BestSeller[];
}

export interface SalesRow {
  date: string;
  invoiceNo: string;
  tableName: string;
  cashierName: string;
  itemCount: number;
  total: number;
  cost: number;
  profit: number;
  paymentMethod: string;
}

/**
 * Day-of-week index (0 = Sunday) for a `yyyy-MM-dd` string.
 *
 * <p>Returns the index rather than a name so the caller can translate it —
 * weekday labels live under the `weekday` namespace in messages/*.json.
 */
export function weekdayIndex(iso: string): number {
  return new Date(`${iso}T00:00:00`).getDay();
}

export function dayOfMonth(iso: string): string {
  return String(new Date(`${iso}T00:00:00`).getDate());
}
