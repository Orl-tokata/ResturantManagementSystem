import Link from "next/link";
import { Button, Card, EmptyState } from "@/components/ui";

/**
 * The sidebar links here, but a receipt only exists for a specific bill.
 * Point the user at the two ways to reach one rather than showing an empty slip.
 */
export default function CashierReceiptPage() {
  return (
    <Card>
      <EmptyState
        icon="🧾"
        title="ជ្រើសរើសវិក្កយបត្រជាមុនសិន"
        description="A receipt belongs to a specific bill. Pick one from the order history, or take a new order and settle it."
        action={
          <div className="flex flex-wrap justify-center gap-2">
            <Link href="/cashier/history">
              <Button variant="primary">🕘 ប្រវត្តិបញ្ជាទិញ · Order history</Button>
            </Link>
            <Link href="/cashier/tables">
              <Button variant="accent">🛒 បញ្ជាទិញថ្មី · New order</Button>
            </Link>
          </div>
        }
      />
    </Card>
  );
}
