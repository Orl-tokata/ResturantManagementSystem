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

/** Khmer weekday initials for chart ticks. */
export const KM_WEEKDAYS = ["អា", "ច", "អ", "ព", "ព្រ", "សុ", "ស"];

export function weekdayKm(iso: string): string {
  return KM_WEEKDAYS[new Date(`${iso}T00:00:00`).getDay()] ?? "";
}

export function dayOfMonth(iso: string): string {
  return String(new Date(`${iso}T00:00:00`).getDate());
}
