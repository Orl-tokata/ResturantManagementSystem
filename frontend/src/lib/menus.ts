import {
  BadgeDollarSign,
  BookUser,
  Boxes,
  ClipboardList,
  Clock,
  Home,
  KeyRound,
  LayoutDashboard,
  type LucideIcon,
  Package,
  Receipt,
  Settings,
  ShoppingCart,
  Tags,
  TrendingUp,
  Truck,
  UserRound,
  Users,
  Utensils,
} from "lucide-react";

export interface MenuItem {
  href: string;
  icon: LucideIcon;
  km: string;
  en: string;
}

/** Ported from MENUS in the prototype's assets/js/proto.js. */
export const ADMIN_MENU: MenuItem[] = [
  { href: "/admin",                 icon: LayoutDashboard, km: "ផ្ទាំងគ្រប់គ្រង",  en: "Dashboard" },
  { href: "/admin/products",        icon: Utensils,        km: "ផលិតផល",          en: "Products" },
  { href: "/admin/categories",      icon: Tags,            km: "ប្រភេទ",           en: "Categories" },
  { href: "/admin/tables",          icon: BookUser,        km: "តុ",               en: "Tables" },
  { href: "/admin/staff",           icon: Users,           km: "បុគ្គលិក",         en: "Staff" },
  { href: "/admin/suppliers",       icon: Truck,           km: "អ្នកផ្គត់ផ្គង់",    en: "Suppliers" },
  { href: "/admin/purchase",        icon: ClipboardList,   km: "ការទិញ",           en: "Purchase" },
  { href: "/admin/stock",           icon: Boxes,           km: "ស្តុក",            en: "Stock" },
  { href: "/admin/reports",         icon: TrendingUp,      km: "របាយការណ៍",       en: "Reports" },
  { href: "/admin/settings",        icon: Settings,        km: "ការកំណត់",         en: "Settings" },
  { href: "/admin/change-password", icon: KeyRound,        km: "ប្តូរពាក្យសម្ងាត់", en: "Password" },
];

export const CASHIER_MENU: MenuItem[] = [
  { href: "/cashier",          icon: Home,             km: "ទំព័រដើម",             en: "Home" },
  { href: "/cashier/order",    icon: ShoppingCart,     km: "បញ្ជាទិញ",             en: "Order" },
  { href: "/cashier/tables",   icon: BookUser,         km: "ជ្រើសរើសតុ",           en: "Tables" },
  { href: "/cashier/payment",  icon: BadgeDollarSign,  km: "ការទូទាត់",            en: "Payment" },
  { href: "/cashier/receipt",  icon: Receipt,          km: "វិក្កយបត្រ",            en: "Receipt" },
  { href: "/cashier/history",  icon: Clock,            km: "ប្រវត្តិ",              en: "History" },
  { href: "/cashier/profile",  icon: UserRound,        km: "ព័ត៌មានផ្ទាល់ខ្លួន",    en: "Profile" },
];

export const MENUS = { admin: ADMIN_MENU, cashier: CASHIER_MENU } as const;
export type ShellVariant = keyof typeof MENUS;

/** Icon shown next to the page title in the topbar. */
export { Package as FallbackIcon };
