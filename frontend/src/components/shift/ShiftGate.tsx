"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { useTranslations } from "next-intl";
import { useOpenShift } from "@/lib/use-shift";

/**
 * No open shift, no POS.
 *
 * <p>The rule that makes every cash figure mean something: a sale belongs to a
 * session, and a session starts with somebody counting the drawer. Without it
 * the shift report is a guess about which takings belong to which cashier.
 *
 * <p>This is the convenience half. The backend refuses to settle a bill
 * without an open shift whatever the screen does — a guard that only redirects
 * is a guard an API call walks straight past. What this adds is that the
 * cashier finds out before they have rung up an order, rather than at the
 * moment the customer is holding out a note.
 *
 * <p>It redirects rather than rendering a message, because the message would
 * have one button on it and SCREENS §3.1 asks for this to cost two taps.
 */
export function ShiftGate({ children }: { children: ReactNode }) {
  const t = useTranslations("common");
  const router = useRouter();
  const pathname = usePathname();
  const { data: shift, isLoading, isError } = useOpenShift();

  const blocked = !isLoading && !isError && !shift;

  useEffect(() => {
    if (blocked) {
      router.replace(`/cashier/shift?next=${encodeURIComponent(pathname)}`);
    }
  }, [blocked, pathname, router]);

  if (isLoading) {
    return (
      <div className="grid min-h-screen place-items-center bg-ink-100">
        <div className="flex items-center gap-3 text-sm text-ink-500">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-teal-600 border-t-transparent" />
          {t("loading")}
        </div>
      </div>
    );
  }

  // A failed query is not a closed till. The backend is the real gate, so
  // letting the screen through on a network error costs nothing — the payment
  // itself will still be refused, with a reason.
  if (blocked) return null;

  return <>{children}</>;
}
