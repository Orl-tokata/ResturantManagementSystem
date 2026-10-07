import type { ReactNode } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";

/**
 * The till, for the people who work it.
 *
 * <p>Sits above both route groups — (shell) for the ordinary screens and (pos)
 * for the order screen, which keeps the whole viewport — so neither can be
 * reached without the right role.
 *
 * <p>The group only asked for a signed-in user before, which was the same
 * thing as "anyone" while every login was an admin or a cashier. It stopped
 * being the same thing the moment the account form offered WAITER and CHEF:
 * those accounts landed here and could ring up sales, take payment, open and
 * close a cash drawer and hand money back. The server now says the same (see
 * OrderController, ShiftController, ReturnController); this stops the client
 * offering what the server would refuse.
 */
export default function CashierLayout({ children }: { children: ReactNode }) {
  return <RequireAuth roles={["ADMIN", "CASHIER"]}>{children}</RequireAuth>;
}
