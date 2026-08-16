import type { ReactNode } from "react";
import { AppShell } from "@/components/layout/AppShell";

/**
 * Wraps the cashier screens in the shared chrome. /cashier/order sits in the
 * sibling (pos) group instead, so the POS keeps the whole viewport.
 */
export default function CashierShellLayout({ children }: { children: ReactNode }) {
  return <AppShell variant="cashier">{children}</AppShell>;
}
