import { describe, expect, it } from "vitest";
import { landingPath } from "@/lib/landing";

/*
 * Where ?next= is allowed to send someone.
 *
 * RequireAuth bounces anyone signed out to /login?next=<where they were>, so
 * the parameter is routinely present and routinely points at a page the person
 * who eventually signs in may not be allowed to open. It is also in the URL
 * bar, so it is whatever anyone cares to type.
 */

describe("landingPath", () => {
  it("sends each role to its own home when there is no next", () => {
    expect(landingPath(null, "ADMIN")).toBe("/admin");
    expect(landingPath(null, "CASHIER")).toBe("/cashier/order");
    expect(landingPath(null, "WAITER")).toBe("/cashier/order");
    expect(landingPath(null, "CHEF")).toBe("/cashier/order");
  });

  it("honours a local path the user may open", () => {
    expect(landingPath("/cashier/order", "CASHIER")).toBe("/cashier/order");
    expect(landingPath("/admin/staff", "ADMIN")).toBe("/admin/staff");
    expect(landingPath("/cashier/history?date=2026-09-24", "WAITER")).toBe(
      "/cashier/history?date=2026-09-24",
    );
  });

  it("refuses to bounce a non-admin back into the admin area", () => {
    // The sequence this exists for: a cashier opens /admin/staff, RequireAuth
    // sends them to /login?next=%2Fadmin%2Fstaff, they sign in, and without
    // this they land back on a screen that only shows them a refusal.
    expect(landingPath("/admin/staff", "CASHIER")).toBe("/cashier/order");
    expect(landingPath("/admin", "WAITER")).toBe("/cashier/order");
    expect(landingPath("/admin/report/sales", "CHEF")).toBe("/cashier/order");
  });

  it("does not mistake a lookalike path for the admin area", () => {
    // /administration is not under app/(protected)/admin, so nothing about it
    // is admin-only. A prefix test without the boundary would refuse it.
    expect(landingPath("/administration", "CASHIER")).toBe("/administration");
  });

  it("refuses anything that is not a local path", () => {
    // An open redirect on a login screen is worth more to a phisher than the
    // login screen itself: the link really does go to the restaurant's domain.
    for (const hostile of [
      "https://evil.example/steal",
      "//evil.example/steal",
      "http://evil.example",
      "javascript:alert(1)",
      "evil.example",
      "",
    ]) {
      expect(landingPath(hostile, "ADMIN")).toBe("/admin");
      expect(landingPath(hostile, "CASHIER")).toBe("/cashier/order");
    }
  });
});
