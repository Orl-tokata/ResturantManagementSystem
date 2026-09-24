import { describe, expect, it } from "vitest";
import type { ReactElement } from "react";
import AdminLayout from "./layout";
import { RequireAuth } from "@/components/auth/RequireAuth";

/*
 * That the admin section asks for the admin role.
 *
 * RequireAuth.test.tsx proves the guard refuses the wrong role; this proves the
 * guard is actually wrapped around these routes with a role named. Those are
 * different failures, and it was the second one that happened: the section
 * inherited the group's guard, which asks for a signed-in user and nothing
 * more, so a cashier who typed /admin/staff got the admin navigation and a page
 * full of empty tables where the backend had returned 403.
 *
 * The layout is called rather than rendered — it is a composition, so the
 * element it returns says everything, and the test does not need a locale
 * provider or a router to read it.
 */

describe("the admin section", () => {
  it("is guarded, and guarded by role", () => {
    const tree = AdminLayout({ children: null }) as ReactElement<{ roles?: string[] }>;

    expect(tree.type).toBe(RequireAuth);
    expect(tree.props.roles).toEqual(["ADMIN"]);
  });
});
