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

/* ---- Bilingual labels ----------------------------------------------------- */

export const ZONE_LABEL: Record<TableZone, string> = {
  INDOOR: "ខាងក្នុង · Indoor",
  OUTDOOR: "ខាងក្រៅ · Outdoor",
  VIP: "បន្ទប់ VIP · VIP",
};

export const TABLE_STATUS_LABEL: Record<TableStatus, string> = {
  FREE: "ទំនេរ · Free",
  OCCUPIED: "កំពុងប្រើ · Occupied",
  RESERVED: "កក់ទុក · Reserved",
};

export const RECORD_STATUS_LABEL: Record<RecordStatus, string> = {
  ACTIVE: "សកម្ម · Active",
  INACTIVE: "មិនសកម្ម · Inactive",
};

export const ROLE_LABEL: Record<Role, string> = {
  ADMIN: "អ្នកគ្រប់គ្រង · Admin",
  CASHIER: "អ្នកគិតលុយ · Cashier",
  WAITER: "អ្នករត់តុ · Waiter",
  CHEF: "ចុងភៅ · Chef",
};

export const SHIFT_LABEL: Record<Shift, string> = {
  MORNING: "ព្រឹក · Morning",
  EVENING: "ល្ងាច · Evening",
  FULL_TIME: "ពេញម៉ោង · Full time",
};

export const STAFF_STATUS_LABEL: Record<StaffStatus, string> = {
  ACTIVE: "សកម្ម · Active",
  ON_LEAVE: "ឈប់សម្រាក · On leave",
  RESIGNED: "ឈប់ធ្វើការ · Resigned",
};

export const GENDER_LABEL: Record<Gender, string> = {
  MALE: "ប្រុស · Male",
  FEMALE: "ស្រី · Female",
};
