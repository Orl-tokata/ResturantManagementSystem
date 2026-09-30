"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Field, Input } from "@/components/ui";
import { useApiError } from "@/lib/use-api-error";
import { formatKhr, formatUsd } from "@/lib/format";
import { get, newIdempotencyKey, post } from "@/lib/api";
import { tenderState } from "@/lib/payment";
import { KhqrPanel } from "@/components/pos/KhqrPanel";
import { PAYMENT_ICON, type Order, type PaymentMethod } from "@/types/order";

/**
 * Settling a bill, inside the POS rather than on a screen of its own.
 *
 * <p>Taking payment used to mean leaving the order screen for
 * `/cashier/payment` and coming back on failure — four screens and three
 * navigations for the most repeated action in the building. The bill stays on
 * screen here, and the cashier's eyes stay in one place.
 *
 * <p>docs/SCREENS.md §2.1.
 */

/**
 * Shown until the till says what it offers.
 *
 * <p>KHQR is omitted on purpose: showing it and taking it away when the answer
 * arrives is worse than showing it a moment late, because a cashier may already
 * have tapped it.
 */
const METHODS_WHILE_LOADING: PaymentMethod[] = ["CASH", "CARD", "TRANSFER"];
const KEYS = ["7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "⌫"];
const QUICK_TENDERS = [1, 5, 10, 20, 50, 100];

export function PaymentPanel({
  order,
  onPaid,
  onBack,
}: {
  order: Order;
  onPaid: () => void;
  onBack: () => void;
}) {
  const t = useTranslations("payment");
  const tc = useTranslations("common");
  const tPay = useTranslations("enum.paymentMethod");
  const apiError = useApiError();

  const [method, setMethod] = useState<PaymentMethod>("CASH");
  const [tendered, setTendered] = useState("");
  const [error, setError] = useState<string | null>(null);

  /*
   * Which methods this till can actually carry out. KHQR is absent unless a
   * Bakong account is configured — offering a method the server will refuse
   * means the cashier finds out only after tapping it, in front of a customer.
   */
  const methods = useQuery({
    queryKey: ["payment-methods"],
    queryFn: () => get<PaymentMethod[]>("/orders/payment-methods"),
    staleTime: Infinity,
    retry: false,
  });
  const available = methods.data ?? METHODS_WHILE_LOADING;

  /*
   * One key for the life of this panel, so a retry after a timeout settles the
   * same bill rather than a second one. Regenerated only when the panel is
   * reopened, which is a new intent.
   */
  const [idempotencyKey] = useState(newIdempotencyKey);

  const state = tenderState({
    total: order.total,
    tendered,
    method,
    status: order.status,
    itemCount: order.items.length,
  });

  const payment = useMutation({
    mutationFn: () =>
      post<Order>(
        `/orders/${order.id}/pay`,
        {
          paymentMethod: method,
          // Only cash carries a tender; the others settle the exact total.
          amountTendered: method === "CASH" ? state.amount : undefined,
        },
        idempotencyKey,
      ),
    onSuccess: onPaid,
    onError: (e) => setError(apiError(e, "paymentRejected")),
  });

  function press(key: string) {
    setError(null);
    if (key === "⌫") {
      setTendered((v) => v.slice(0, -1));
      return;
    }
    // One decimal point only, and never more than two decimal places.
    if (key === "." && tendered.includes(".")) return;
    if (/\.\d{2}$/.test(tendered)) return;
    setTendered((v) => v + key);
  }

  const showKhqr = method === "KHQR" && order.status !== "PAID";

  return (
    <div className="shrink-0 border-t-2 border-navy-800 bg-cream-100">
      <div className="max-h-[70vh] overflow-y-auto p-2.5">
        {error && <Alert tone="error">{error}</Alert>}

        {/* ---- what is owed. The server's figures, not the local estimate:
                only it knows the discount and the rounded riel. ---- */}
        <dl className="space-y-1 text-sm">
          <Line label={tc("subtotal")} value={formatUsd(order.subtotal)} />
          {order.discount > 0 && (
            <Line label={tc("discount")} value={`-${formatUsd(order.discount)}`} />
          )}
          <Line label={`${tc("vat")} ${order.vatRate}%`} value={formatUsd(order.vatAmount)} />
          <div className="my-1.5 border-t border-ink-400" />
          <div className="flex items-baseline justify-between">
            <span className="font-semibold">{t("due")}</span>
            <span className="font-[family-name:var(--font-num)] text-2xl font-bold text-danger">
              {formatUsd(order.total)}
            </span>
          </div>
          <Line label={tc("khr")} value={formatKhr(order.total)} muted />
        </dl>

        {/* ---- method ---- */}
        <div className="mt-3 grid grid-cols-4 gap-1.5">
          {available.map((m) => (
            <button
              key={m}
              type="button"
              onClick={() => {
                setMethod(m);
                setError(null);
              }}
              aria-pressed={method === m}
              className={`grid justify-items-center gap-0.5 rounded-md border-2 px-1 py-2 text-[10px] font-semibold ${
                method === m
                  ? "border-teal-600 bg-[#e8f5f5]"
                  : "border-ink-300 bg-white hover:bg-ink-50"
              }`}
            >
              <span className="text-lg">{PAYMENT_ICON[m]}</span>
              {tPay(m)}
            </button>
          ))}
        </div>

        {method === "CASH" && (
          <>
            <div className="mt-3">
              <Field
                label={t("tendered")}
                htmlFor="tendered"
                error={state.shortfall ? t("shortfall") : undefined}
              >
                <Input
                  id="tendered"
                  inputMode="decimal"
                  value={tendered}
                  onChange={(e) => setTendered(e.target.value.replace(/[^\d.]/g, ""))}
                  placeholder="0.00"
                  className="text-right font-[family-name:var(--font-num)] text-xl"
                />
              </Field>
            </div>

            <div className="mb-2 flex items-baseline justify-between text-sm">
              <span>{t("change")}</span>
              <b
                className={`font-[family-name:var(--font-num)] text-lg ${
                  state.change >= 0 ? "text-success" : "text-danger"
                }`}
              >
                {formatUsd(Math.max(0, state.change))}
              </b>
            </div>

            {/* the notes a cashier actually holds */}
            <div className="mb-2 flex flex-wrap gap-1">
              {QUICK_TENDERS.map((n) => (
                <Button key={n} size="sm" variant="light" onClick={() => setTendered(String(n))}>
                  ${n}
                </Button>
              ))}
              <Button
                size="sm"
                variant="light"
                onClick={() => setTendered(order.total.toFixed(2))}
              >
                {t("exact")}
              </Button>
            </div>

            <div className="grid grid-cols-3 gap-1.5">
              {KEYS.map((k) => (
                <button
                  key={k}
                  type="button"
                  onClick={() => press(k)}
                  className={`rounded border border-ink-300 py-2.5 font-[family-name:var(--font-num)] text-lg font-semibold ${
                    k === "⌫"
                      ? "border-transparent bg-orange-500 text-white"
                      : "bg-white hover:bg-ink-100"
                  }`}
                >
                  {k}
                </button>
              ))}
            </div>
          </>
        )}

        {/*
         * KHQR takes over the settle area rather than sitting beside it. There
         * is no "confirm" for a scan-to-pay bill — the money either arrived or
         * it did not, and a button letting a cashier assert otherwise would put
         * back exactly the trust-based step this replaces.
         */}
        {showKhqr ? (
          <div className="mt-3">
            <KhqrPanel orderId={order.id} onPaid={onPaid} />
          </div>
        ) : (
          <div className="mt-3 space-y-1.5">
            {!state.canPay && state.blocker && (
              <p className="rounded border border-ink-300 bg-white px-3 py-2 text-center text-xs text-ink-700">
                {t(state.blocker)}
              </p>
            )}
            <Button
              variant="admin"
              block
              size="lg"
              disabled={!state.canPay}
              loading={payment.isPending}
              onClick={() => payment.mutate()}
            >
              ✔ {t("confirm")}
            </Button>
          </div>
        )}

        <Button variant="ghost" block className="mt-1.5" onClick={onBack}>
          {t("backToOrder")}
        </Button>
      </div>
    </div>
  );
}

function Line({ label, value, muted = false }: { label: string; value: string; muted?: boolean }) {
  return (
    <div className={`flex justify-between ${muted ? "text-xs text-ink-500" : ""}`}>
      <span>{label}</span>
      <b className="font-[family-name:var(--font-num)]">{value}</b>
    </div>
  );
}
