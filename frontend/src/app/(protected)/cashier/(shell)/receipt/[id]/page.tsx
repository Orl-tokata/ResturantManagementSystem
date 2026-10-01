"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";
import { Printer, ShoppingCart } from "lucide-react";
import { useTranslations } from "next-intl";
import { Alert, Button, Input } from "@/components/ui";
import { Barcode } from "@/components/pos/Barcode";
import { get, type PageResponse } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import { useBilingual, useBilingualPair } from "@/i18n/bilingual";
// formatKhr converts from USD at the current rate. A receipt must show the riel
// figure the customer actually paid, which the server stored on the order — so
// totalKhr is printed as-is rather than recomputed.
import { formatReceiptDateTime, formatUsd } from "@/lib/format";
import type { Order, Receipt } from "@/types/order";

export default function ReceiptPage() {
  const t = useTranslations("receipt");
  const tc = useTranslations("common");
  const tH = useTranslations("history");
  const tCh = useTranslations("cashierHome");
  const apiError = useApiError();

  /*
   * The slip prints both languages. Everywhere else one is right, because the
   * person reading chose it; a receipt is handed to a customer nobody asked,
   * and may end up in front of a bank or an accountant. The cashier's language
   * leads and the other follows.
   */
  const bi = useBilingual();
  const biPair = useBilingualPair();

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
  const paidAt = formatReceiptDateTime(order.paidAt);

  return (
    <>
      {/* Toolbar is screen-only; the print stylesheet drops it. */}
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2 print:hidden">
        <div className="flex flex-wrap items-center gap-2">
          <Link href="/cashier/history">
            <Button variant="ghost">← {tH("title")}</Button>
          </Link>
          <InvoiceLookup />
        </div>
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
          {phone && <div>{bi("common.phone")}: {phone}</div>}
        </div>

        <Dashes />

        <Row label={bi("receipt.invoice")} value={order.invoiceNo} />
        <Row label={bi("receipt.table")} value={order.tableName ?? "—"} />
        {order.guestCount != null && (
          <Row label={bi("receipt.guests")} value={String(order.guestCount)} />
        )}
        <Row label={bi("receipt.cashier")} value={order.cashierName ?? "—"} />
        <Row label={bi("common.date")} value={paidAt} />

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

        <Row label={bi("common.subtotal")} value={formatUsd(order.subtotal)} />
        {order.discount > 0 && (
          <Row label={bi("common.discount")} value={`-${formatUsd(order.discount)}`} />
        )}
        <Row label={`${bi("common.vat")} ${order.vatRate}%`} value={formatUsd(order.vatAmount)} />
        <div className="flex justify-between gap-3 text-[15px] font-bold">
          <span>{bi("receipt.totalCaps")}</span>
          <span className="font-[family-name:var(--font-num)] tabular-nums">
            {formatUsd(order.total)}
          </span>
        </div>
        <Row label={bi("common.khr")} value={`${order.totalKhr.toLocaleString()} ៛`} />

        {order.status === "PAID" && (
          <>
            <Dashes />
            <Row
              label={
                order.paymentMethod
                  ? bi(`enum.paymentMethod.${order.paymentMethod}`)
                  : "—"
              }
              value={formatUsd(order.amountTendered ?? 0)}
            />
            <Row label={bi("payment.change")} value={formatUsd(order.changeAmount ?? 0)} />
          </>
        )}

        <Dashes />

        <div className="text-center">
          {/* Sentences, so they stack instead of being joined with a slash. */}
          {biPair("receipt.thanks").map(
            (line, i) => line && <div key={i}>{line}</div>,
          )}
          {biPair("receipt.comeAgain").map(
            (line, i) => line && <div key={i}>{line}</div>,
          )}
          {/*
            * A real Code 128 of the invoice number, and the number in text
            * beneath it so the slip stays useful to a person as well as a
            * scanner. What stood here originally was a run of pipe characters
            * that looked like a barcode and encoded nothing.
            */}
          <Barcode value={order.invoiceNo} className="mx-auto mt-3" height={38} />
          <div className="font-[family-name:var(--font-num)] tracking-[2px]">
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


/**
 * Jumping to another bill's slip without leaving this screen.
 *
 * <p>The Receipt entry lands on the most recent sale, which is what a cashier
 * usually wants — but a customer coming back a day later arrives holding a
 * number, and until this the only way to use it was to go to the history,
 * search, and come back. The slip in their hand is the search term.
 *
 * <p>Matches the way the history searches: a partial number works, so "219"
 * finds INV-00219 without anyone typing the prefix.
 */
function InvoiceLookup() {
  const t = useTranslations("receipt");
  const tH = useTranslations("history");
  const router = useRouter();

  const [term, setTerm] = useState("");
  const [busy, setBusy] = useState(false);
  const [missing, setMissing] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    const q = term.trim();
    if (!q) return;

    setBusy(true);
    setMissing(false);
    try {
      const page = await get<PageResponse<Order>>("/orders", { search: q, size: 1 });
      const found = page.content[0];
      if (!found) {
        setMissing(true);
        return;
      }
      setTerm("");
      router.push(`/cashier/receipt/${found.id}`);
    } catch {
      // A failed lookup is not a failed receipt: the slip on screen is still
      // correct and printable, so this says so quietly rather than replacing
      // the page with an error.
      setMissing(true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="flex items-center gap-1.5">
      <Input
        value={term}
        onChange={(e) => {
          setTerm(e.target.value);
          setMissing(false);
        }}
        placeholder={tH("invoiceNo")}
        aria-label={t("lookupLabel")}
        className="w-36 py-1.5 text-sm"
      />
      <Button type="submit" variant="light" size="sm" loading={busy} disabled={!term.trim()}>
        {t("open")}
      </Button>
      {missing && (
        <span role="status" className="text-xs font-semibold text-danger">
          {t("notFound")}
        </span>
      )}
    </form>
  );
}
