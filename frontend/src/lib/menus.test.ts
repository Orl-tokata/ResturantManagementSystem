import { describe, expect, it } from "vitest";

import { ADMIN_MENU, CASHIER_MENU, activeMenuItem } from "./menus";

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
