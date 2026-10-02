"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Printer, ShoppingCart } from "lucide-react";
import { useTranslations } from "next-intl";
import {
  Alert,
  Badge,
  Button,
  Pagination,
  SearchBar,
  Select,
  toneForOrderStatus,
} from "@/components/ui";
import { Barcode } from "@/components/pos/Barcode";
import { useList } from "@/hooks/useCrud";
import { get } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import { useBilingual, useBilingualPair } from "@/i18n/bilingual";
// formatKhr converts from USD at the current rate. A receipt must show the riel
// figure the customer actually paid, which the server stored on the order — so
// totalKhr is printed as-is rather than recomputed.
import { formatReceiptDateTime, formatUsd } from "@/lib/format";
import type { Order, Receipt } from "@/types/order";

/**
 * Where the picker starts. One of the sizes the chooser offers, so the control
 * does not open showing a blank for a value that is not on its own list.
 */
const INITIAL_SIZE = 20;

/** The statuses worth filtering a receipt list by. */
const STATUSES = ["PAID", "OPEN", "CANCELLED"] as const;

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
    /*
     * The bills on the left, the one being read on the right.
     *
     * `print:block` collapses the grid when printing, and the list carries
     * `print:hidden` — so the paper gets the slip and nothing else, whatever is
     * on screen beside it.
     */
    <div className="grid gap-4 lg:h-full lg:min-h-0 lg:grid-cols-[clamp(240px,24vw,320px)_1fr] print:block print:h-auto print:overflow-visible">
      <InvoiceList currentId={id} />

      {/*
        * Scrolls itself rather than letting the page scroll.
        *
        * A slip is taller than the window — this one is 688px in a 614px
        * content area — so something has to scroll. Keeping it inside this
        * column means the toolbar, the list and the app chrome all stay put
        * while it does, instead of the whole screen sliding and taking Print
        * off the top with it.
        *
        * Only from `lg`: below that the two panels stack, and a phone scrolling
        * its page is the right behaviour, not a bug to design around.
        */}
      <div className="lg:h-full lg:min-h-0 lg:overflow-y-auto print:h-auto print:overflow-visible">
      {/* Toolbar is screen-only; the print stylesheet drops it. */}
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2 print:hidden">
        <div className="flex flex-wrap items-center gap-2">
          <Link href="/cashier/history">
            <Button variant="ghost">← {tH("title")}</Button>
          </Link>
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

        {/*
          * A line per payment, because a bill can be settled by more than one.
          * It used to print the method against the amount tendered, which on a
          * card read "CARD  $0.00": the server had recorded the total as
          * tendered to fill a column, and the receipt printed the fiction.
          *
          * Tendered and change appear only when cash was involved. They are the
          * cash line's figures, and on a card sale the question does not apply.
          */}
        {order.status === "PAID" && (
          <>
            <Dashes />
            {order.payments
              .filter((p) => p.status === "CAPTURED")
              .map((p) => (
                <Row
                  key={p.id}
                  label={bi(`enum.paymentMethod.${p.method}`)}
                  value={formatUsd(p.amount)}
                />
              ))}
            {order.amountTendered != null && (
              <Row label={bi("payment.tendered")} value={formatUsd(order.amountTendered)} />
            )}
            {order.changeAmount != null && (
              <Row label={bi("payment.change")} value={formatUsd(order.changeAmount)} />
            )}
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
      </div>
    </div>
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
 * Every bill, with the one being read marked.
 *
 * <p>The Receipt screen used to show exactly one slip — the latest — and the
 * only way to another was out to the history and back. A customer returning
 * with yesterday's bill is an ordinary thing, so the bills belong on the screen
 * whose job is printing them.
 *
 * <p>Carries `print:hidden`: the paper gets the slip and nothing else.
 */
function InvoiceList({ currentId }: { currentId: string }) {
  const t = useTranslations("receipt");
  const tH = useTranslations("history");
  const tc = useTranslations("common");
  const tStatus = useTranslations("enum.orderStatus");
  const router = useRouter();

  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(INITIAL_SIZE);

  // Empty values are dropped before the query string is built, so "" really
  // means no filter rather than a filter on nothing.
  const list = useList<Order>("orders", { search, status, page, size });
  const rows = list.data?.content ?? [];

  /** Any filter change goes back to the first page; page 4 of a narrower result
      is an empty list that reads as a fault. */
  function filter<T>(set: (v: T) => void) {
    return (value: T) => {
      set(value);
      setPage(0);
    };
  }

  return (
    <aside className="flex min-h-0 flex-col rounded-md border border-ink-200 bg-white p-2.5 lg:h-full print:hidden">
      <SearchBar
        value={search}
        onChange={filter(setSearch)}
        placeholder={tH("invoiceNo")}
      />

      <Select
        value={status}
        onChange={(e) => filter(setStatus)(e.target.value)}
        aria-label={tc("status")}
        className="mt-1.5 py-1.5 text-xs"
      >
        <option value="">{tH("allStatus")}</option>
        {STATUSES.map((v) => (
          <option key={v} value={v}>
            {tStatus(v)}
          </option>
        ))}
      </Select>

      {/* Capped rather than page-length, so the slip beside it stays put as the
          list is searched instead of the whole page growing and shrinking. */}
      {/* Capped while stacked, and simply the leftover height once the grid
          gives the column one. */}
      <div className="mt-2 min-h-0 max-h-[min(60vh,32rem)] flex-1 overflow-y-auto lg:max-h-none">
        {list.isLoading && (
          <p className="py-6 text-center text-xs text-ink-500">{tH("title")}…</p>
        )}

        {!list.isLoading && rows.length === 0 && (
          <p className="py-6 text-center text-xs text-ink-500">{t("notFound")}</p>
        )}

        <ul className="space-y-1">
          {rows.map((o) => {
            const current = String(o.id) === currentId;
            return (
              <li key={o.id}>
                <button
                  type="button"
                  onClick={() => router.push(`/cashier/receipt/${o.id}`)}
                  aria-current={current ? "true" : undefined}
                  className={`w-full rounded border px-2 py-1.5 text-left text-xs transition ${
                    current
                      ? "border-teal-600 bg-[#e8f5f5] font-semibold"
                      : "border-transparent hover:border-ink-200 hover:bg-ink-50"
                  }`}
                >
                  <div className="flex items-baseline justify-between gap-2">
                    <span className="font-[family-name:var(--font-num)]">{o.invoiceNo}</span>
                    <span className="font-[family-name:var(--font-num)] font-semibold">
                      {formatUsd(o.total)}
                    </span>
                  </div>
                  <div className="mt-0.5 flex items-center justify-between gap-2 text-[11px] text-ink-500">
                    <span className="truncate">{o.tableName ?? "—"}</span>
                    <Badge tone={toneForOrderStatus(o.status)}>{tStatus(o.status)}</Badge>
                  </div>
                </button>
              </li>
            );
          })}
        </ul>
      </div>

      <Pagination
        page={list.data?.page ?? 0}
        totalPages={list.data?.totalPages ?? 0}
        totalElements={list.data?.totalElements ?? 0}
        size={list.data?.size ?? size}
        onPage={setPage}
        onSize={filter(setSize)}
      />
    </aside>
  );
}
