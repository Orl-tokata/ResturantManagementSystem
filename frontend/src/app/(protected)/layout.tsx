import type { ReactNode } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";

/**
 * Everything under this group requires a signed-in user.
 * Milestone 5 replaces the bare wrapper with the sidebar/topbar AppShell.
 */
export default function ProtectedLayout({ children }: { children: ReactNode }) {
  return <RequireAuth>{children}</RequireAuth>;
}
