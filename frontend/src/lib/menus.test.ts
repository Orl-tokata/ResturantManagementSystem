import { describe, expect, it } from "vitest";

import messages from "../../messages/en.json";
import {
  ADMIN_ACCOUNT_MENU,
  ADMIN_GROUPS,
  ADMIN_MENU,
  CASHIER_MENU,
  activeGroup,
  activeMenuItem,
} from "./menus";

/**
 * The header title and the sidebar highlight both come from this. It shipped
 * wrong — every admin sub-page said "Dashboard" — because nothing checked it.
 */
describe("activeMenuItem", () => {
  it("picks the sub-page, not the section root that prefixes it", () => {
    // The bug: "/admin" is first in the menu and prefixes every admin path.
    expect(activeMenuItem("/admin/products", ADMIN_MENU)?.key).toBe("products");
    expect(activeMenuItem("/admin/audit", ADMIN_MENU)?.key).toBe("audit");
    expect(activeMenuItem("/admin/change-password", ADMIN_MENU)?.key).toBe("password");
  });

  it("still matches the section root exactly", () => {
    expect(activeMenuItem("/admin", ADMIN_MENU)?.key).toBe("dashboard");
    expect(activeMenuItem("/cashier", CASHIER_MENU)?.key).toBe("home");
  });

  it("matches a nested route to its nearest ancestor", () => {
    // /admin/stock/movements arrives with PLAN.md P4 and must not read as
    // Dashboard, nor need its own menu entry to be titled correctly.
    expect(activeMenuItem("/admin/stock/movements", ADMIN_MENU)?.key).toBe("stock");
    expect(activeMenuItem("/cashier/receipt/42", CASHIER_MENU)?.key).toBe("receipt");
  });

  it("does not match a path that merely starts with the same letters", () => {
    expect(activeMenuItem("/admin/stocktake", ADMIN_MENU)?.key).not.toBe("stock");
  });

  it("returns undefined for a path outside the menu", () => {
    expect(activeMenuItem("/login", ADMIN_MENU)).toBeUndefined();
    expect(activeMenuItem("/admin/products", CASHIER_MENU)).toBeUndefined();
  });
});

/*
 * The grouping of SCREENS §2.2.
 *
 * The failure worth guarding against is not a wrong heading, it is a screen
 * that belongs to none: a new page added to the flat list and forgotten in the
 * groups would simply stop appearing in the sidebar, and nothing else would
 * say so.
 */
describe("ADMIN_GROUPS", () => {
  it("accounts for every admin link exactly once", () => {
    const grouped = ADMIN_GROUPS.flatMap((g) => g.items);
    const all = [...grouped, ...ADMIN_ACCOUNT_MENU];

    expect(new Set(all.map((i) => i.href)).size).toBe(all.length);
    expect(all.map((i) => i.href).sort()).toEqual(ADMIN_MENU.map((i) => i.href).sort());
  });

  it("gives every heading a name in both catalogues", () => {
    // Looked up as tGroup(group.key), which check-messages cannot see: a
    // dynamic key is invisible to it and shows up as a crash on the screen.
    for (const group of ADMIN_GROUPS) {
      expect(messages.nav.group).toHaveProperty(group.key);
    }
  });

  it("opens the heading the current page is inside", () => {
    expect(activeGroup("/admin/products")?.key).toBe("catalog");
    expect(activeGroup("/admin/stock")?.key).toBe("stock");
    expect(activeGroup("/admin/stock/anything")?.key).toBe("stock");
    expect(activeGroup("/admin")?.key).toBe("sell");
  });

  it("has no heading for a page outside the groups", () => {
    // Your own account sits outside them, so nothing should open for it.
    expect(activeGroup("/admin/change-password")).toBeUndefined();
    expect(activeGroup("/login")).toBeUndefined();
  });
});
