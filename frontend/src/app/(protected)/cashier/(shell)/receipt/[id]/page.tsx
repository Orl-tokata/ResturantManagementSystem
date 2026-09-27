"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Printer, ShoppingCart } from "lucide-react";
import { useTranslations } from "next-intl";
import { Alert, Button } from "@/components/ui";
import { get } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
// formatKhr converts from USD at the current rate. A receipt must show the riel
// figure the customer actually paid, which the server stored on the order — so
// totalKhr is printed as-is rather than recomputed.
import { formatUsd } from "@/lib/format";
import type { Receipt } from "@/types/order";

export default function ReceiptPage() {
  const t = useTranslations("receipt");
  const tc = useTranslations("common");
  const tH = useTranslations("history");
  const tCh = useTranslations("cashierHome");
  const tPay = useTranslations("enum.paymentMethod");
  const tP = useTranslations("payment");
  const apiError = useApiError();

  const { id } = useParams<{ id: string }>();

  const receipt = useQuery({
    queryKey: ["receipt", id],
    queryFn: () => get<Receipt>(`/orders/${id}/receipt`),
    retry: false,
  });

  if (receipt.isLoading) {
    return <p className="text-sm text-ink-500">{tc("loading")}</p>;
  }

  if (receipt.isError || !receipt.data) {
    return (
      <Alert tone="error">
        {apiError(receipt.error, "loadReceipt")}{" "}
        <Link href="/cashier/history" className="underline">
          {tH("title")}
        </Link>
      </Alert>
    );
  }

  const { restaurantName, restaurantNameEn, address, phone, order } = receipt.data;
  const paidAt = order.paidAt ? new Date(order.paidAt).toLocaleString() : "—";

  return (
    <>
      {/* Toolbar is screen-only; the print stylesheet drops it. */}
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2 print:hidden">
        <Link href="/cashier/history">
          <Button variant="ghost">← {tH("title")}</Button>
        </Link>
        <div className="flex gap-2">
          <Button variant="light" onClick={() => window.print()}>
            <Printer size={15} /> {tc("print")}
          </Button>
          <Link href="/cashier/tables">
            <Button variant="accent">
              <ShoppingCart size={15} /> {tCh("newOrder")}
            </Button>
          </Link>
        </div>
      </div>

      {order.status !== "PAID" && (
        <div className="mx-auto mb-3 max-w-85 print:hidden">
          <Alert tone="info">
            {t("notPaid")}
          </Alert>
        </div>
      )}

      {/*
        * 80mm thermal-paper proportions.
        *
        * Not font-mono. The slip used to set the whole receipt in the monospace
        * stack — ui-monospace, Menlo, Consolas and the rest — none of which
        * carries a single Khmer glyph. The browser silently substituted some
        * other installed font for every Khmer run: not the monospace, and not
        * the app's own face either. Measured on this machine the same Khmer
        * string came out 129.64px in the mono stack against 102.15px in the
        * real face, and "កកកក" and "ណណណណ" rendered at different widths while
        * "iiii" and "WWWW" matched — a monospace font that is not monospacing
        * Khmer is a font that has no Khmer in it.
        *
        * On this machine that substitute happened to exist. On a till with a
        * different font set, or driving a printer, it need not. A receipt is
        * the one artefact a customer takes away, so it is the worst place to
        * find out.
        *
        * Money still needs to line up, so the amounts use tabular figures
        * (font-num) while the words use the Khmer face.
        */}
      <div className="receipt-slip mx-auto w-85 max-w-full border border-ink-300 bg-white px-5 py-6 text-xs leading-relaxed shadow-md print:border-0 print:shadow-none">
        <div className="text-center">
          <div className="text-[15px] font-bold">{restaurantName}</div>
          <div>{restaurantNameEn}</div>
          {address && <div className="mt-1">{address}</div>}
          {phone && <div>{tc("phone")}: {phone}</div>}
        </div>

        <Dashes />

        <Row label={t("invoice")} value={order.invoiceNo} />
        <Row label={t("table")} value={order.tableName ?? "—"} />
        {order.guestCount != null && (
          <Row label={t("guests")} value={String(order.guestCount)} />
        )}
        <Row label={t("cashier")} value={order.cashierName ?? "—"} />
        <Row label={tc("date")} value={paidAt} />

        <Dashes />

        <table className="w-full">
          <tbody>
            {order.items.map((item, i) => (
              <tr key={item.id ?? i}>
                <td className="py-0.5 align-top">
                  <div className="font-bold">{item.productName}</div>
                  <div className="pl-2 font-[family-name:var(--font-num)] tabular-nums text-ink-500">
                    {item.qty} × {formatUsd(item.unitPrice)}
                  </div>
                  {item.note && <div className="pl-2 italic text-ink-500">— {item.note}</div>}
                </td>
                <td className="py-0.5 text-right align-top font-[family-name:var(--font-num)] tabular-nums">
                  {formatUsd(item.lineTotal)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <Dashes />

        <Row label={tc("subtotal")} value={formatUsd(order.subtotal)} />
        {order.discount > 0 && (
          <Row label={tc("discount")} value={`-${formatUsd(order.discount)}`} />
        )}
        <Row label={`${tc("vat")} ${order.vatRate}%`} value={formatUsd(order.vatAmount)} />
        <div className="flex justify-between text-[15px] font-bold">
          <span>{t("totalCaps")}</span>
          <span className="font-[family-name:var(--font-num)] tabular-nums">
            {formatUsd(order.total)}
          </span>
        </div>
        <Row label={tc("khr")} value={`${order.totalKhr.toLocaleString()} ៛`} />

        {order.status === "PAID" && (
          <>
            <Dashes />
            <Row
              label={order.paymentMethod ? tPay(order.paymentMethod) : "—"}
              value={formatUsd(order.amountTendered ?? 0)}
            />
            <Row label={tP("change")} value={formatUsd(order.changeAmount ?? 0)} />
          </>
        )}

        <Dashes />

        <div className="text-center">
          <div>{t("thanks")}</div>
          <div>{t("comeAgain")}</div>
          {/*
            * The invoice number, not a picture of a barcode. What stood here
            * was a run of pipe characters that looked like one and encoded
            * nothing — a scanner reads it as the text "||||| |||| || ||||| |||"
            * or, more often, refuses it. Printing the number plainly is honest
            * and is what anyone reconciling by hand actually needs. A real
            * Code128 belongs here if a scanner is ever put on the counter.
            */}
          <div className="mt-2 font-[family-name:var(--font-num)] tracking-[2px]">
            {order.invoiceNo}
          </div>
        </div>
      </div>
    </>
  );
}

/**
 * A label and its value.
 *
 * <p>The value carries the tabular face so figures in successive rows line up
 * on the decimal point, which is the only thing the monospace was buying on a
 * slip that is otherwise mostly words.
 */
function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-3">
      <span>{label}</span>
      <span className="text-right font-[family-name:var(--font-num)] tabular-nums">{value}</span>
    </div>
  );
}

function Dashes() {
  return <div className="my-2 border-t border-dashed border-ink-500" />;
}
