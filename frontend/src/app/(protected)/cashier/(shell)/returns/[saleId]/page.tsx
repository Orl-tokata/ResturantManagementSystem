"use client";

import { useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  Alert,
  Button,
  Card,
  type Column,
  DataTable,
  Field,
  Input,
  ListPage,
  Select,
  Textarea,
  Toolbar,
} from "@/components/ui";
import { get, newIdempotencyKey, post } from "@/lib/api";
import { formatUsd } from "@/lib/format";
import { useApiError } from "@/lib/use-api-error";
import { PAYMENT_METHODS, type PaymentMethod } from "@/types/order";
import type { ReturnableLine, ReturnableOrder, SaleReturn } from "@/types/returns";

export default function ReturnPage() {
  const saleId = Number(useParams().saleId);
  const t = useTranslations("returns");
  const tc = useTranslations("common");
  const tPay = useTranslations("enum.paymentMethod");
  const apiError = useApiError();

  /** How many of each line the cashier has picked, by order-item id. */
  const [picked, setPicked] = useState<Record<number, number>>({});
  const [reason, setReason] = useState("");
  const [method, setMethod] = useState<PaymentMethod | "">("");
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState<SaleReturn | null>(null);

  /*
   * One key for the whole visit to this screen, not one per attempt.
   *
   * A refund is the one action where a retry after a timeout must not produce
   * a second document — the money would leave the drawer twice and the slip
   * the customer is holding would only account for half of it.
   */
  const [idempotencyKey] = useState(newIdempotencyKey);

  const sale = useQuery({
    queryKey: ["returnable", saleId],
    queryFn: () => get<ReturnableOrder>(`/orders/${saleId}/returnable`),
    enabled: Number.isFinite(saleId),
  });

  const submit = useMutation({
    mutationFn: () =>
      post<SaleReturn>(
        "/returns",
        {
          orderId: saleId,
          lines: Object.entries(picked)
            .filter(([, qty]) => qty > 0)
            .map(([orderItemId, qty]) => ({ orderItemId: Number(orderItemId), qty })),
          reason: reason.trim(),
          refundMethod: method || undefined,
        },
        idempotencyKey,
      ),
    onSuccess: setDone,
    onError: (e) => setError(apiError(e, "createReturn")),
  });

  const lines = sale.data?.lines ?? [];
  const chosen = lines.filter((l) => (picked[l.orderItemId] ?? 0) > 0);

  /*
   * An estimate, and labelled as one. The server works the refund out as a
   * share of what was actually paid — VAT and any discount are spread across
   * the lines, so the sum of menu prices is not what the customer gets back.
   * Showing a figure here is still worth it: a cashier needs to know roughly
   * what is leaving the drawer before they commit to it.
   */
  const estimate = chosen.reduce(
    (sum, l) => sum + l.unitPrice * (picked[l.orderItemId] ?? 0),
    0,
  );

  function setQty(line: ReturnableLine, qty: number) {
    const clamped = Math.max(0, Math.min(qty, line.remainingQty));
    setPicked((p) => ({ ...p, [line.orderItemId]: clamped }));
  }

  const columns: Column<ReturnableLine>[] = [
    { key: "name", header: tc("name"), render: (l) => <span className="font-medium">{l.productName}</span> },
    { key: "sold", header: t("sold"), numeric: true, render: (l) => l.soldQty },
    {
      key: "already",
      header: t("alreadyReturned"),
      numeric: true,
      hideOnMobile: true,
      render: (l) => (l.returnedQty > 0 ? l.returnedQty : "—"),
    },
    { key: "left", header: t("remaining"), numeric: true, render: (l) => l.remainingQty },
    { key: "price", header: tc("price"), numeric: true, hideOnMobile: true, render: (l) => formatUsd(l.unitPrice) },
    {
      key: "qty",
      header: t("giveBack"),
      align: "right",
      render: (l) => (
        <div className="flex items-center justify-end gap-1.5">
          <Button
            size="sm"
            variant="light"
            disabled={(picked[l.orderItemId] ?? 0) <= 0}
            onClick={() => setQty(l, (picked[l.orderItemId] ?? 0) - 1)}
            aria-label={t("oneLess", { name: l.productName })}
          >
            −
          </Button>
          <Input
            type="number"
            min="0"
            max={l.remainingQty}
            className="w-16 text-center"
            value={picked[l.orderItemId] ?? 0}
            onChange={(e) => setQty(l, Number(e.target.value))}
            // A line with nothing left is shown, not hidden: "you already
            // returned both of these" is the answer the cashier came for.
            disabled={l.remainingQty <= 0}
          />
          <Button
            size="sm"
            variant="light"
            disabled={(picked[l.orderItemId] ?? 0) >= l.remainingQty}
            onClick={() => setQty(l, (picked[l.orderItemId] ?? 0) + 1)}
            aria-label={t("oneMore", { name: l.productName })}
          >
            +
          </Button>
        </div>
      ),
    },
  ];

  /* ---- Done ------------------------------------------------------------- */

  if (done) {
    return (
      <ListPage>
        <Card
          title={`${t("slip")} — ${done.returnNo}`}
          className="mx-auto w-full max-w-md"
          action={
            <Button variant="ghost" onClick={() => window.print()}>
              🖨️ {tc("print")}
            </Button>
          }
        >
          <dl className="space-y-1.5 text-sm">
            <Row label={t("against")} value={done.invoiceNo} />
            <Row label={tc("date")} value={new Date(done.createdAt).toLocaleString()} />
            <Row label={t("method")} value={tPay(done.refundMethod)} />
            {done.approvedBy && <Row label={t("approvedBy")} value={done.approvedBy} />}
            <Row label={tc("reason")} value={done.reason} />
          </dl>

          <ul className="mt-3 border-t border-ink-200 pt-2 text-sm">
            {done.items.map((i) => (
              <li key={i.id} className="flex justify-between py-0.5">
                <span>
                  {i.qty} × {i.productName}
                </span>
                <span className="font-[family-name:var(--font-num)]">{formatUsd(i.lineTotal)}</span>
              </li>
            ))}
          </ul>

          <div className="mt-2 flex justify-between border-t-2 border-ink-400 pt-2 text-lg font-bold text-danger">
            <span>{t("refunded")}</span>
            <span className="font-[family-name:var(--font-num)]">{formatUsd(done.total)}</span>
          </div>

          <div className="mt-4 flex gap-2">
            <Link href={`/cashier/receipt/${done.orderId}`} className="flex-1">
              <Button variant="light" className="w-full">
                {t("viewSale")}
              </Button>
            </Link>
            <Link href="/cashier/history" className="flex-1">
              <Button variant="primary" className="w-full">
                {tc("done")}
              </Button>
            </Link>
          </div>
        </Card>
      </ListPage>
    );
  }

  /* ---- Picking ----------------------------------------------------------- */

  return (
    <ListPage>
      {sale.isError && <Alert tone="error">{apiError(sale.error, "loadSale")}</Alert>}
      {error && <Alert tone="error">{error}</Alert>}

      {sale.data && !sale.data.anythingLeft && (
        <Alert tone="warn">{t("nothingLeft")}</Alert>
      )}

      <Toolbar
        left={
          <Link href={`/cashier/receipt/${saleId}`}>
            <Button variant="light">← {t("backToSale")}</Button>
          </Link>
        }
        right={
          <span className="text-sm text-ink-500">
            {t("against")} <b className="text-ink-900">{sale.data?.invoiceNo ?? "—"}</b>
          </span>
        }
      />

      <DataTable
        fill
        columns={columns}
        rows={lines}
        rowKey={(l) => l.orderItemId}
        loading={sale.isLoading}
        emptyMessage={t("noLines")}
      />

      <Card className="mt-3">
        <div className="grid gap-3 sm:grid-cols-2">
          {/* Mandatory, and the first field rather than the last: a refund
              nobody explained is the entry that cannot be accounted for. */}
          <Field label={tc("reason")} htmlFor="r-reason" required hint={t("reasonHint")}>
            <Textarea
              id="r-reason"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder={t("reasonPlaceholder")}
            />
          </Field>

          <div>
            <Field label={t("method")} htmlFor="r-method" hint={t("methodHint")}>
              <Select
                id="r-method"
                value={method}
                onChange={(e) => setMethod(e.target.value as PaymentMethod | "")}
              >
                <option value="">
                  {sale.data?.originalMethod
                    ? `${t("sameAsSale")} — ${tPay(sale.data.originalMethod)}`
                    : t("sameAsSale")}
                </option>
                {PAYMENT_METHODS.map((m) => (
                  <option key={m} value={m}>
                    {tPay(m)}
                  </option>
                ))}
              </Select>
            </Field>

            <div className="mt-3 flex items-baseline justify-between border-t border-ink-200 pt-3">
              <span className="text-sm text-ink-500">{t("estimate")}</span>
              <b className="font-[family-name:var(--font-num)] text-xl text-danger">
                {formatUsd(estimate)}
              </b>
            </div>
            <p className="mt-1 text-xs text-ink-500">{t("estimateNote")}</p>

            <Button
              variant="danger"
              className="mt-3 w-full py-3"
              loading={submit.isPending}
              disabled={chosen.length === 0 || !reason.trim()}
              onClick={() => {
                setError(null);
                submit.mutate();
              }}
            >
              {t("refund")}
            </Button>
          </div>
        </div>
      </Card>
    </ListPage>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-3">
      <dt className="text-ink-500">{label}</dt>
      <dd className="text-right font-medium">{value}</dd>
    </div>
  );
}
