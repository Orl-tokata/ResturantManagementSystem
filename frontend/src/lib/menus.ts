import {
  BookUser,
  Boxes,
  ClipboardList,
  Clock,
  FileClock,
  Home,
  Contact,
  KeyRound,
  LayoutDashboard,
  type LucideIcon,
  Receipt,
  ShoppingCart,
  Percent,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Wallet,
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

/** A heading and the links under it. */
export interface MenuGroup {
  /** Key under the `nav.group` namespace in messages/*.json. */
  key: string;
  items: MenuItem[];
}

/**
 * The admin navigation, in five headings.
 *
 * <p>SCREENS §2.2: the flat list reached seventeen items, and a scroll is a
 * menu nobody reads. The headings are not a tidy-up of the old order — they
 * are how an owner thinks about the business, so "where do I put prices up"
 * has one obvious place to look rather than four plausible ones.
 *
 * <p>Some of what §2.2 lists does not exist as a screen: returns and shifts
 * are the cashier's, loyalty lives inside a customer, stock movements became a
 * tab on the stock screen, and there are no roles, tax or printer pages.
 * Nothing is invented here to fill a heading out.
 */
export const ADMIN_GROUPS: MenuGroup[] = [
  {
    key: "sell",
    items: [
      { href: "/admin", icon: LayoutDashboard, key: "dashboard" },
      { href: "/admin/reports", icon: TrendingUp, key: "reports" },
    ],
  },
  {
    key: "catalog",
    items: [
      { href: "/admin/products", icon: Utensils, key: "products" },
      { href: "/admin/categories", icon: Tags, key: "categories" },
      { href: "/admin/modifiers", icon: SlidersHorizontal, key: "modifiers" },
      { href: "/admin/promotions", icon: Percent, key: "promotions" },
    ],
  },
  {
    key: "stock",
    items: [
      { href: "/admin/stock", icon: Boxes, key: "stock" },
      { href: "/admin/purchase", icon: ClipboardList, key: "purchase" },
      { href: "/admin/suppliers", icon: Truck, key: "suppliers" },
    ],
  },
  {
    key: "people",
    items: [
      { href: "/admin/customers", icon: Contact, key: "customers" },
      { href: "/admin/staff", icon: Users, key: "staff" },
      { href: "/admin/users", icon: ShieldCheck, key: "users" },
    ],
  },
  {
    key: "setup",
    items: [
      { href: "/admin/tables", icon: BookUser, key: "tables" },
      { href: "/admin/settings", icon: Settings, key: "settings" },
      { href: "/admin/audit", icon: FileClock, key: "audit" },
    ],
  },
];

/**
 * Your own account, outside the headings.
 *
 * <p>Neither of these is a part of running the restaurant, and putting them
 * under "Setup" would mean opening a section about the business to change your
 * own password. They sit at the foot of the sidebar instead.
 */
export const ADMIN_ACCOUNT_MENU: MenuItem[] = [
  { href: "/admin/profile", icon: UserRound, key: "profile" },
  { href: "/admin/change-password", icon: KeyRound, key: "password" },
];

/**
 * Every admin link, flat.
 *
 * <p>Derived rather than written out again: {@code activeMenuItem} and the
 * page header need one list, and two copies would drift the first time a
 * screen moved between headings.
 */
export const ADMIN_MENU: MenuItem[] = [
  ...ADMIN_GROUPS.flatMap((group) => group.items),
  ...ADMIN_ACCOUNT_MENU,
];

/*
 * "Order" points at the table picker, not at /cashier/order.
 *
 * Ordering happens against a table, so /cashier/order needs a tableId and a
 * menu link has none — the old entry landed on a dead end every time, with no
 * sidebar to leave by. But the label was never the problem: a cashier looks for
 * where to take an order, not for furniture, and the admin menu already uses
 * "Tables" for managing the table list, which is a different job.
 *
 * So the label names the task and the route is the screen that starts it.
 */
/**
 * Flat, on purpose. SCREENS §2.2 groups the admin sidebar and leaves this one
 * alone: it is seven items, a cashier learns them in a day, and a heading to
 * open before reaching the till would be friction on the most-used screen in
 * the building.
 */
export const CASHIER_MENU: MenuItem[] = [
  { href: "/cashier", icon: Home, key: "home" },
  { href: "/cashier/shift", icon: Wallet, key: "shift" },
  { href: "/cashier/tables", icon: ShoppingCart, key: "order" },
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
/** The heading that contains a path, or undefined. */
export function activeGroup(pathname: string): MenuGroup | undefined {
  const item = activeMenuItem(pathname, ADMIN_MENU);
  if (!item) return undefined;
  return ADMIN_GROUPS.find((group) => group.items.includes(item));
}

export function activeMenuItem(
  pathname: string,
  items: MenuItem[],
): MenuItem | undefined {
  return items
    .filter((m) => pathname === m.href || pathname.startsWith(`${m.href}/`))
    .sort((a, b) => b.href.length - a.href.length)[0];
}
