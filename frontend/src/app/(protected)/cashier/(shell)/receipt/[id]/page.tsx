"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { Printer, ShoppingCart } from "lucide-react";
import { useTranslations } from "next-intl";
import { Alert, Button } from "@/components/ui";
import { get } from "@/lib/api";
import { errorMessage } from "@/lib/errors";
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
        {errorMessage(receipt.error, "Could not load the receipt")}{" "}
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

      {/* 80mm thermal-paper proportions, monospaced like a real till slip. */}
      <div className="mx-auto w-85 max-w-full border border-ink-300 bg-white px-5 py-6 font-mono text-xs leading-relaxed shadow-md print:border-0 print:shadow-none">
        <div className="text-center">
          <div className="text-[15px] font-bold">{restaurantName}</div>
          <div>{restaurantNameEn}</div>
          {address && <div className="mt-1">{address}</div>}
          {phone && <div>Tel: {phone}</div>}
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
                  <div className="pl-2 text-ink-500">
                    {item.qty} × {formatUsd(item.unitPrice)}
                  </div>
                  {item.note && <div className="pl-2 italic text-ink-500">— {item.note}</div>}
                </td>
                <td className="py-0.5 text-right align-top">{formatUsd(item.lineTotal)}</td>
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
          <span>{formatUsd(order.total)}</span>
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
          <div className="mt-2 tracking-[2px]">||||| |||| || ||||| |||</div>
          <div>{order.invoiceNo}</div>
        </div>
      </div>
    </>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-3">
      <span>{label}</span>
      <span className="text-right">{value}</span>
    </div>
  );
}

function Dashes() {
  return <div className="my-2 border-t border-dashed border-ink-500" />;
}
