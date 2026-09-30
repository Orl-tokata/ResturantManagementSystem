"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useTranslations } from "next-intl";
import { useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, EmptyState } from "@/components/ui";
import { get, type PageResponse } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import type { Order } from "@/types/order";

/**
 * The sidebar's Receipt entry.
 *
 * <p>A receipt belongs to one bill, so this path has no receipt of its own. It
 * used to say "Choose a receipt first" and offer two links — on every single
 * visit, because there was nothing else it could ever say.
 *
 * <p>What a cashier almost always wants from here is the sale they just took:
 * a customer comes back for the slip, or the printer ate it. So it goes
 * straight to the most recent settled bill, and only shows an empty state when
 * there genuinely are no sales to show.
 */
export default function CashierReceiptPage() {
  const t = useTranslations("receipt");
  const tCh = useTranslations("cashierHome");
  const router = useRouter();
  const apiError = useApiError();

  /*
   * One page of settled bills, newest-opened first.
   *
   * Asking for a page rather than a single row because the server orders by
   * when a bill was opened, and the last bill *paid* is not always the last
   * one opened — a table that sat for an hour settles after one that arrived
   * later. Picking the newest paidAt from a page gets it right without a new
   * endpoint. Beyond a page of reordering it would not, which no real service
   * produces.
   */
  const paid = useQuery({
    queryKey: ["orders", { status: "PAID", size: 20 }],
    queryFn: () => get<PageResponse<Order>>("/orders", { status: "PAID", size: 20 }),
    retry: false,
  });

  const latest = (paid.data?.content ?? [])
    .filter((o) => o.paidAt)
    .sort((a, b) => (a.paidAt! < b.paidAt! ? 1 : -1))[0];

  useEffect(() => {
    if (latest) router.replace(`/cashier/receipt/${latest.id}`);
  }, [latest, router]);

  if (paid.isError) {
    return <Alert tone="error">{apiError(paid.error, "loadOrder")}</Alert>;
  }

  // Loading, or about to redirect. Either way there is no news for the reader,
  // and a placeholder that flashes for a moment is worse than a quiet one.
  if (paid.isLoading || latest) {
    return <p className="text-sm text-ink-500">…</p>;
  }

  return (
    <Card>
      <EmptyState
        icon="🧾"
        title={t("noSalesYet")}
        description={t("noSalesHelp")}
        action={
          <Link href="/cashier/tables">
            <Button variant="accent">🛒 {tCh("newOrder")}</Button>
          </Link>
        }
      />
    </Card>
  );
}
