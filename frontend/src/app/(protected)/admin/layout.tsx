import type { ReactNode } from "react";
import { AppShell } from "@/components/layout/AppShell";
import { RequireAuth } from "@/components/auth/RequireAuth";

/**
 * Admin screens, for admins.
 *
 * <p>The group's own layout only asks for a signed-in user, which let a
 * cashier open every admin screen — the backend refused the data, so they saw
 * empty tables rather than anything they should not, but they got the admin
 * navigation and a broken page. Authorisation belongs in both places: the
 * server decides, and the client should not offer what the server will refuse.
 */
export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <RequireAuth roles={["ADMIN"]}>
      <AppShell variant="admin">{children}</AppShell>
    </RequireAuth>
  );
}
