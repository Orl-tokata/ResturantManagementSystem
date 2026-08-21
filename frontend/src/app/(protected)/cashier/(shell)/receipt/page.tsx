import Link from "next/link";
import { getTranslations } from "next-intl/server";
import { Button, Card, EmptyState } from "@/components/ui";

/**
 * The sidebar links here, but a receipt only exists for a specific bill.
 * Point the user at the two ways to reach one rather than showing an empty slip.
 */
export default async function CashierReceiptPage() {
  const t = await getTranslations("receipt");
  const tH = await getTranslations("history");
  const tCh = await getTranslations("cashierHome");

  return (
    <Card>
      <EmptyState
        icon="🧾"
        title={t("pickFirst")}
        description={t("pickHelp")}
        action={
          <div className="flex flex-wrap justify-center gap-2">
            <Link href="/cashier/history">
              <Button variant="primary">🕘 {tH("title")}</Button>
            </Link>
            <Link href="/cashier/tables">
              <Button variant="accent">🛒 {tCh("newOrder")}</Button>
            </Link>
          </div>
        }
      />
    </Card>
  );
}
