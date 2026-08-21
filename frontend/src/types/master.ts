import type { Role } from "@/types/auth";

export type RecordStatus = "ACTIVE" | "INACTIVE";

/* ---- Category ------------------------------------------------------------ */

export interface Category {
  id: number;
  name: string;
  nameEn: string | null;
  icon: string | null;
  sortOrder: number;
  status: RecordStatus;
  productCount: number;
}

export interface CategoryRequest {
  name: string;
  nameEn?: string;
  icon?: string;
  sortOrder?: number;
  status?: RecordStatus;
}

/* ---- Product ------------------------------------------------------------- */

export interface Product {
  id: number;
  name: string;
  nameEn: string | null;
  categoryId: number;
  categoryName: string;
  price: number;
  cost: number;
  stockQty: number;
  imageUrl: string | null;
  description: string | null;
  status: RecordStatus;
}

export interface ProductRequest {
  name: string;
  nameEn?: string;
  categoryId: number;
  price: number;
  cost?: number;
  stockQty?: number;
  imageUrl?: string;
  description?: string;
  status?: RecordStatus;
}

/* ---- Dining table -------------------------------------------------------- */

export type TableZone = "INDOOR" | "OUTDOOR" | "VIP";
export type TableStatus = "FREE" | "OCCUPIED" | "RESERVED";

export interface DiningTable {
  id: number;
  name: string;
  seats: number;
  zone: TableZone;
  status: TableStatus;
}

export interface TableRequest {
  name: string;
  seats: number;
  zone?: TableZone;
  status?: TableStatus;
}

export interface TableSummary {
  free: number;
  occupied: number;
  reserved: number;
  total: number;
}

/* ---- Staff --------------------------------------------------------------- */

export type Gender = "MALE" | "FEMALE";
export type Shift = "MORNING" | "EVENING" | "FULL_TIME";
export type StaffStatus = "ACTIVE" | "ON_LEAVE" | "RESIGNED";

export interface Staff {
  id: number;
  staffCode: string;
  staffName: string;
  gender: Gender | null;
  dateOfBirth: string | null;
  phone: string | null;
  email: string | null;
  role: Role;
  shift: Shift | null;
  salary: number | null;
  hireDate: string | null;
  address: string | null;
  status: StaffStatus;
  userId: number | null;
}

export interface StaffRequest {
  staffCode: string;
  staffName: string;
  gender?: Gender;
  dateOfBirth?: string;
  phone?: string;
  email?: string;
  role: Role;
  shift?: Shift;
  salary?: number;
  hireDate?: string;
  address?: string;
  status?: StaffStatus;
}
