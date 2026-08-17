"use client";

import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Suspense, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Card, DataTable, Field, Input, type Column } from "@/components/ui";
import { get, post } from "@/lib/api";
import { errorMessage } from "@/lib/errors";
import { formatKhr, formatUsd } from "@/lib/format";
import {
  PAYMENT_ICON,
  PAYMENT_LABEL,
  type Order,
  type OrderItem,
  type PaymentMethod,
} from "@/types/order";

const METHODS: PaymentMethod[] = ["CASH", "CARD", "KHQR", "TRANSFER"];
const KEYS = ["7", "8", "9", "4", "5", "6", "1", "2", "3", "0", ".", "⌫"];

function PaymentScreen() {
  const params = useSearchParams();
  const router = useRouter();
  const orderId = Number(params.get("orderId") || 0);

  const [method, setMethod] = useState<PaymentMethod>("CASH");
  const [tendered, setTendered] = useState("");
  const [error, setError] = useState<string | null>(null);

  const order = useQuery({
    queryKey: ["order", orderId],
    queryFn: () => get<Order>(`/orders/${orderId}`),
    enabled: orderId > 0,
    retry: false,
  });

  const payment = useMutation({
    mutationFn: () =>
      post<Order>(`/orders/${orderId}/pay`, {
        paymentMethod: method,
        // Only cash carries a tender; the others settle the exact total.
        amountTendered: method === "CASH" ? Number(tendered || 0) : undefined,
      }),
    onSuccess: () => router.replace(`/cashier/receipt/${orderId}`),
    onError: (e) => setError(errorMessage(e, "Payment was not accepted")),
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

  const total = order.data?.total ?? 0;
  const tenderedNum = Number(tendered || 0);
  const change = tenderedNum - total;
  const shortfall = method === "CASH" && tendered !== "" && change < 0;
  const canPay =
    !!order.data &&
    order.data.status === "OPEN" &&
    order.data.items.length > 0 &&
    (method !== "CASH" || (tendered !== "" && change >= 0));

  const columns: Column<OrderItem>[] = [
    { key: "name", header: "ទំនិញ · Item", render: (r) => r.productName },
    { key: "qty", header: "ចំនួន", numeric: true, render: (r) => r.qty },
    { key: "price", header: "តម្លៃ", numeric: true, render: (r) => formatUsd(r.unitPrice) },
    { key: "sum", header: "សរុប", numeric: true, render: (r) => formatUsd(r.lineTotal) },
  ];

  if (!orderId) {
    return (
      <Alert tone="error">
        គ្មានវិក្កយបត្រ · No order selected.{" "}
        <Link href="/cashier/tables" className="underline">
          ជ្រើសរើសតុ · Pick a table
        </Link>
      </Alert>
    );
  }

  if (order.isError) {
    return <Alert tone="error">{errorMessage(order.error, "Could not load the order")}</Alert>;
  }

  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_320px] lg:items-start">
      {/* ---- left: what is being paid for ---- */}
      <div className="space-y-4">
        <Card
          title={`វិក្កយបត្រ ${order.data?.invoiceNo ?? "…"} · ${order.data?.tableName ?? ""}`}
          action={<span className="text-xs text-ink-500">{order.data?.items.length ?? 0} មុខ</span>}
          padded={false}
        >
          <DataTable
            columns={columns}
            rows={order.data?.items ?? []}
            rowKey={(r, i) => r.id ?? i}
            loading={order.isLoading}
          />
        </Card>

        <Card title="វិធីបង់ប្រាក់ · Payment method">
          <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-4">
            {METHODS.map((m) => (
              <button
                key={m}
                type="button"
                onClick={() => {
                  setMethod(m);
                  setError(null);
                }}
                aria-pressed={method === m}
                className={`grid justify-items-center gap-1 rounded-md border-2 px-2 py-3 text-xs font-semibold ${
                  method === m
                    ? "border-teal-600 bg-[#e8f5f5]"
                    : "border-ink-300 bg-white hover:bg-ink-50"
                }`}
              >
                <span className="text-xl">{PAYMENT_ICON[m]}</span>
                {PAYMENT_LABEL[m]}
              </button>
            ))}
          </div>
        </Card>
      </div>

      {/* ---- right: the money ---- */}
      <div className="space-y-4">
        {error && <Alert tone="error">{error}</Alert>}

        {order.data && order.data.status !== "OPEN" && (
          <Alert tone="info">
            វិក្កយបត្រនេះ {order.data.status === "PAID" ? "បានបង់រួច" : "បានលុប"} · This bill is
            already {order.data.status.toLowerCase()}.{" "}
            <Link href={`/cashier/receipt/${orderId}`} className="underline">
              មើលវិក្កយបត្រ · View receipt
            </Link>
          </Alert>
        )}

        <Card>
          <dl className="space-y-1.5 text-sm">
            <Line label="សរុបរង · Subtotal" value={formatUsd(order.data?.subtotal ?? 0)} />
            {(order.data?.discount ?? 0) > 0 && (
              <Line label="បញ្ចុះតម្លៃ · Discount" value={`-${formatUsd(order.data!.discount)}`} />
            )}
            <Line
              label={`ពន្ធ · VAT ${order.data?.vatRate ?? 0}%`}
              value={formatUsd(order.data?.vatAmount ?? 0)}
            />
            <div className="my-2 border-t border-ink-200" />
            <div className="flex items-baseline justify-between">
              <span className="font-semibold">ត្រូវបង់ · Due</span>
              <span className="font-[family-name:var(--font-num)] text-2xl font-bold text-danger">
                {formatUsd(total)}
              </span>
            </div>
            <Line label="រៀល · KHR" value={formatKhr(total)} muted />
          </dl>

          {method === "CASH" && (
            <>
              <div className="mt-4">
                <Field
                  label="ទឹកប្រាក់ទទួល · Amount tendered"
                  htmlFor="tendered"
                  error={shortfall ? "តិចជាងចំនួនត្រូវបង់ · Less than the amount due" : undefined}
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

              <div className="mb-3 flex items-baseline justify-between text-sm">
                <span>ប្រាក់អាប់ · Change</span>
                <b
                  className={`font-[family-name:var(--font-num)] text-lg ${
                    change >= 0 ? "text-success" : "text-danger"
                  }`}
                >
                  {formatUsd(Math.max(0, change))}
                </b>
              </div>

              {/* quick tenders — the notes a cashier actually holds */}
              <div className="mb-2 flex flex-wrap gap-1.5">
                {[1, 5, 10, 20, 50, 100].map((n) => (
                  <Button key={n} size="sm" variant="light" onClick={() => setTendered(String(n))}>
                    ${n}
                  </Button>
                ))}
                <Button size="sm" variant="light" onClick={() => setTendered(total.toFixed(2))}>
                  ត្រង់ · Exact
                </Button>
              </div>

              <div className="grid grid-cols-3 gap-1.5">
                {KEYS.map((k) => (
                  <button
                    key={k}
                    type="button"
                    onClick={() => press(k)}
                    className={`rounded border border-ink-300 py-3 font-[family-name:var(--font-num)] text-lg font-semibold ${
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

          <div className="mt-4 space-y-2">
            <Button
              variant="admin"
              block
              size="lg"
              disabled={!canPay}
              loading={payment.isPending}
              onClick={() => payment.mutate()}
            >
              ✔ បញ្ជាក់ការទូទាត់ · Confirm payment
            </Button>
            <Button
              variant="ghost"
              block
              onClick={() => router.push(`/cashier/order?tableId=${order.data?.tableId ?? ""}`)}
            >
              ត្រឡប់ក្រោយ · Back to order
            </Button>
          </div>
        </Card>
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

export default function CashierPaymentPage() {
  return (
    <Suspense fallback={<p className="text-sm text-ink-500">កំពុងផ្ទុក… · Loading</p>}>
      <PaymentScreen />
    </Suspense>
  );
}
