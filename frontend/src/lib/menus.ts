import {
  BookUser,
  Boxes,
  ClipboardList,
  Clock,
  FileClock,
  Home,
  KeyRound,
  LayoutDashboard,
  type LucideIcon,
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
  /** Key under the `nav` namespace in messages/*.json. */
  key: string;
}

/** Ported from MENUS in the prototype's assets/js/proto.js. */
export const ADMIN_MENU: MenuItem[] = [
  { href: "/admin", icon: LayoutDashboard, key: "dashboard" },
  { href: "/admin/products", icon: Utensils, key: "products" },
  { href: "/admin/categories", icon: Tags, key: "categories" },
  { href: "/admin/tables", icon: BookUser, key: "tables" },
  { href: "/admin/staff", icon: Users, key: "staff" },
  { href: "/admin/suppliers", icon: Truck, key: "suppliers" },
  { href: "/admin/purchase", icon: ClipboardList, key: "purchase" },
  { href: "/admin/stock", icon: Boxes, key: "stock" },
  { href: "/admin/reports", icon: TrendingUp, key: "reports" },
  { href: "/admin/audit", icon: FileClock, key: "audit" },
  { href: "/admin/settings", icon: Settings, key: "settings" },
  { href: "/admin/change-password", icon: KeyRound, key: "password" },
];

export const CASHIER_MENU: MenuItem[] = [
  { href: "/cashier", icon: Home, key: "home" },
  { href: "/cashier/order", icon: ShoppingCart, key: "order" },
  { href: "/cashier/tables", icon: BookUser, key: "tables" },
  { href: "/cashier/receipt", icon: Receipt, key: "receipt" },
  { href: "/cashier/history", icon: Clock, key: "history" },
  { href: "/cashier/profile", icon: UserRound, key: "profile" },
];

export const MENUS = { admin: ADMIN_MENU, cashier: CASHIER_MENU } as const;
export type ShellVariant = keyof typeof MENUS;

/**
 * The menu item a path belongs to, or undefined.
 *
 * Longest match, not the first one. `find` returned the dashboard for every
 * page, because "/admin" is first in the menu and prefixes all of them — so the
 * sidebar highlighted Products while the header said Dashboard, on every admin
 * and cashier sub-page, unnoticed.
 *
 * Longest match also handles a route nested under another item, such as the
 * stock movements page in docs/PLAN.md P4, which the sidebar's own rule
 * (exclude "/admin" and "/cashier" from prefix matching) would not.
 */
export function activeMenuItem(
  pathname: string,
  items: MenuItem[],
): MenuItem | undefined {
  return items
    .filter((m) => pathname === m.href || pathname.startsWith(`${m.href}/`))
    .sort((a, b) => b.href.length - a.href.length)[0];
}
