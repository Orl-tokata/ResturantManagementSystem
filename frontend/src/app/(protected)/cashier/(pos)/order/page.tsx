"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Minus, Plus, Trash2, X } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { Clock } from "@/components/layout/Clock";
import { Alert, Button, SearchBar } from "@/components/ui";
import { get, post, put, type PageResponse } from "@/lib/api";
import { errorMessage } from "@/lib/errors";
import { pickName } from "@/i18n/name";
import { formatKhr, formatUsd } from "@/lib/format";
import { useAuth } from "@/lib/auth-context";
import type { Category, Product } from "@/types/master";
import type { CartLine, Order } from "@/types/order";

function PosScreen() {
  const params = useSearchParams();
  const router = useRouter();
  const qc = useQueryClient();
  const { user } = useAuth();

  const tableId = Number(params.get("tableId") || 0);

  const t = useTranslations("pos");
  const tc = useTranslations("common");
  const locale = useLocale();

  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [search, setSearch] = useState("");
  const [cart, setCart] = useState<CartLine[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [dirty, setDirty] = useState(false);

  /* ---- Data ---------------------------------------------------------- */

  const categories = useQuery({
    queryKey: ["categories", "active"],
    queryFn: () => get<Category[]>("/categories/active"),
    staleTime: 5 * 60_000,
  });

  const products = useQuery({
    queryKey: ["products", { categoryId, search, size: 200 }],
    queryFn: () =>
      get<PageResponse<Product>>("/products", {
        ...(categoryId ? { categoryId } : {}),
        ...(search ? { search } : {}),
        size: 200,
      }),
    placeholderData: (prev) => prev,
  });

  // Opening is idempotent per table on the server, so this both creates the
  // bill and recovers the existing one after a reload.
  const order = useQuery({
    queryKey: ["order", "table", tableId],
    queryFn: () => post<Order>("/orders", { tableId }),
    enabled: tableId > 0,
    staleTime: Infinity,
    retry: false,
  });

  // Seed the basket from the server exactly once per bill; after that the local
  // cart is the source of truth until it is saved. Adjusted during render
  // rather than in an effect, so the first paint already shows the lines.
  const [seededFor, setSeededFor] = useState<number | null>(null);
  if (order.data && seededFor !== order.data.id) {
    setSeededFor(order.data.id);
    setCart(
      order.data.items.map((i) => ({
        productId: i.productId ?? 0,
        productName: i.productName,
        unitPrice: i.unitPrice,
        qty: i.qty,
        note: i.note ?? undefined,
      })),
    );
    setDirty(false);
  }

  /* ---- Mutations ------------------------------------------------------ */

  const saveItems = useMutation({
    mutationFn: (lines: CartLine[]) =>
      put<Order>(`/orders/${order.data!.id}/items`, {
        items: lines.map((l) => ({ productId: l.productId, qty: l.qty, note: l.note })),
      }),
    onSuccess: (updated) => {
      qc.setQueryData(["order", "table", tableId], updated);
      setDirty(false);
    },
  });

  const cancelOrder = useMutation({
    mutationFn: () => post<Order>(`/orders/${order.data!.id}/cancel`),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["tables"] });
      router.push("/cashier/tables");
    },
  });

  /* ---- Cart operations ------------------------------------------------ */

  function addProduct(p: Product) {
    setCart((lines) => {
      const found = lines.find((l) => l.productId === p.id);
      if (found) {
        return lines.map((l) => (l.productId === p.id ? { ...l, qty: l.qty + 1 } : l));
      }
      return [
        ...lines,
        { productId: p.id, productName: p.name, unitPrice: p.price, qty: 1 },
      ];
    });
    setDirty(true);
  }

  function changeQty(productId: number, delta: number) {
    setCart((lines) =>
      lines
        .map((l) => (l.productId === productId ? { ...l, qty: l.qty + delta } : l))
        // Decrementing to zero removes the line, which is what a cashier expects.
        .filter((l) => l.qty > 0),
    );
    setDirty(true);
  }

  function removeLine(productId: number) {
    setCart((lines) => lines.filter((l) => l.productId !== productId));
    setDirty(true);
  }

  /* ---- Local totals ---------------------------------------------------
     Shown live while the cashier taps. The server recomputes on save and its
     numbers win — these exist so the panel is not blank between requests.  */

  const totals = useMemo(() => {
    const subtotal = cart.reduce((sum, l) => sum + l.unitPrice * l.qty, 0);
    const rate = order.data?.vatRate ?? 10;
    const vat = (subtotal * rate) / 100;
    return { subtotal, vat, total: subtotal + vat, rate };
  }, [cart, order.data?.vatRate]);

  async function saveThenPay() {
    setError(null);
    try {
      if (dirty) await saveItems.mutateAsync(cart);
      router.push(`/cashier/payment?orderId=${order.data!.id}`);
    } catch (e) {
      setError(errorMessage(e, "Could not save the order"));
    }
  }

  /* ---- No table chosen ------------------------------------------------- */

  if (!tableId) {
    return (
      <div className="grid min-h-screen place-items-center bg-ink-100 p-6">
        <div className="max-w-md rounded-md border border-ink-200 bg-white p-8 text-center shadow-sm">
          <div className="mb-2 text-4xl">🪑</div>
          <h1 className="mb-1 text-lg font-bold">{t("pickTableFirst")}</h1>
          <Link href="/cashier/tables">
            <Button variant="accent" size="lg">
              {t("backToTables")}
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-ink-200">
      {/* ---- top strip ---- */}
      <div className="flex shrink-0 items-center gap-2.5 bg-navy-800 px-3 py-2 text-white">
        <Link
          href="/cashier/tables"
          aria-label="Back to tables"
          className="grid h-8 w-8 place-items-center rounded text-orange-500 hover:bg-white/15"
        >
          <X size={18} />
        </Link>

        <div className="min-w-0 flex-1">
          <SearchBar
            value={search}
            onChange={setSearch}
            placeholder={t("searchDishes")}
            className="max-w-64 border-white/25 bg-white/10 text-white [&_input]:text-white [&_input]:placeholder:text-white/60"
          />
        </div>

        <div className="shrink-0 text-right text-xs leading-tight">
          <b className="block text-sm">
            {order.data?.tableName ?? `Table ${tableId}`}
          </b>
          <span className="text-white/70">{order.data?.invoiceNo ?? "…"}</span>
        </div>
      </div>

      {error && (
        <div className="px-3 pt-2">
          <Alert tone="error">{error}</Alert>
        </div>
      )}
      {order.isError && (
        <div className="px-3 pt-2">
          <Alert tone="error">{errorMessage(order.error, "Could not open the bill")}</Alert>
        </div>
      )}

      {/* ---- body ---- */}
      <div className="grid min-h-0 flex-1 grid-cols-1 md:grid-cols-[92px_1fr_330px]">
        {/* category rail */}
        <div className="scroll-invert hidden flex-col gap-1.5 overflow-y-auto bg-teal-800 p-1.5 md:flex">
          <button
            type="button"
            onClick={() => setCategoryId(null)}
            className={`rounded px-1.5 py-2.5 text-xs font-semibold leading-tight text-white ${
              categoryId === null ? "bg-orange-500" : "bg-white/12 hover:bg-white/25"
            }`}
          >
            {tc("all")}
          </button>

          {categories.data?.map((c) => (
            <button
              key={c.id}
              type="button"
              onClick={() => setCategoryId(c.id)}
              className={`rounded px-1.5 py-2.5 text-xs font-semibold leading-tight text-white ${
                categoryId === c.id ? "bg-orange-500" : "bg-white/12 hover:bg-white/25"
              }`}
            >
              <span className="block text-base">{c.icon}</span>
              {pickName(locale, c.name, c.nameEn)}
            </button>
          ))}
        </div>

        {/* product grid */}
        <div className="grid auto-rows-min grid-cols-[repeat(auto-fill,minmax(128px,1fr))] gap-2.5 overflow-y-auto bg-ink-100 p-3">
          {products.isLoading && (
            <p className="col-span-full py-10 text-center text-sm text-ink-500">{tc("loading")}</p>
          )}

          {products.data?.content
            .filter((p) => p.status === "ACTIVE")
            .map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => addProduct(p)}
                className="overflow-hidden rounded-md border border-ink-200 bg-white text-left shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"
              >
                <div className="grid h-[78px] place-items-center bg-ink-200 text-3xl">
                  {p.imageUrl || "🍽️"}
                </div>
                <div className="px-2 py-2">
                  <div className="text-xs font-semibold leading-tight">{p.name}</div>
                  <div className="font-[family-name:var(--font-num)] text-sm font-bold text-teal-600">
                    {formatUsd(p.price)}
                  </div>
                </div>
              </button>
            ))}

          {products.data && products.data.content.length === 0 && (
            <p className="col-span-full py-10 text-center text-sm text-ink-500">
              {t("noDishes")}
            </p>
          )}
        </div>

        {/* order panel */}
        <div className="flex min-h-0 flex-col border-l border-ink-300 bg-cream-100">
          <div className="flex items-center justify-between bg-navy-800 px-3 py-2 text-sm font-semibold text-white">
            <span>{t("order")}</span>
            <span className="font-[family-name:var(--font-num)]">{cart.length}</span>
          </div>

          <div className="min-h-0 flex-1 overflow-y-auto">
            {cart.length === 0 ? (
              <p className="p-6 text-center text-xs text-ink-500">
                {t("noItems")}
                <br />
                {t("tapToAdd")}
              </p>
            ) : (
              <table className="w-full text-xs">
                <tbody>
                  {cart.map((l) => (
                    <tr key={l.productId} className="border-b border-dashed border-ink-300">
                      <td className="px-2 py-2">
                        <div className="font-semibold">{l.productName}</div>
                        <div className="text-ink-500">{formatUsd(l.unitPrice)}</div>
                      </td>
                      <td className="px-1 py-2">
                        <div className="flex items-center gap-1">
                          <button
                            type="button"
                            aria-label={`Decrease ${l.productName}`}
                            onClick={() => changeQty(l.productId, -1)}
                            className="grid h-6 w-6 place-items-center rounded border border-ink-300 bg-white"
                          >
                            <Minus size={12} />
                          </button>
                          <span className="w-6 text-center font-[family-name:var(--font-num)] font-bold">
                            {l.qty}
                          </span>
                          <button
                            type="button"
                            aria-label={`Increase ${l.productName}`}
                            onClick={() => changeQty(l.productId, 1)}
                            className="grid h-6 w-6 place-items-center rounded border border-ink-300 bg-white"
                          >
                            <Plus size={12} />
                          </button>
                        </div>
                      </td>
                      <td className="px-2 py-2 text-right font-[family-name:var(--font-num)] font-semibold">
                        {formatUsd(l.unitPrice * l.qty)}
                      </td>
                      <td className="pr-2">
                        <button
                          type="button"
                          aria-label={`Remove ${l.productName}`}
                          onClick={() => removeLine(l.productId)}
                          className="text-danger-soft"
                        >
                          <Trash2 size={13} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          {/* totals */}
          <div className="border-t-2 border-navy-800 bg-[#f7edd8] px-3 py-2.5 text-sm">
            <div className="flex justify-between py-0.5">
              <span>{tc("subtotal")}</span>
              <b className="font-[family-name:var(--font-num)]">{formatUsd(totals.subtotal)}</b>
            </div>
            <div className="flex justify-between py-0.5">
              <span>{tc("vat")} {totals.rate}%</span>
              <b className="font-[family-name:var(--font-num)]">{formatUsd(totals.vat)}</b>
            </div>
            <div className="mt-1 flex justify-between border-t border-ink-400 pt-1.5 text-lg font-bold text-danger">
              <span>{tc("total")}</span>
              <span className="font-[family-name:var(--font-num)]">{formatUsd(totals.total)}</span>
            </div>
            <div className="flex justify-between text-xs text-ink-500">
              <span>{tc("khr")}</span>
              <span className="font-[family-name:var(--font-num)]">{formatKhr(totals.total)}</span>
            </div>
            {dirty && (
              <p className="mt-1.5 text-center text-xs font-semibold text-warning">
                {t("unsaved")}
              </p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-2 p-2.5">
            <Button
              variant="light"
              onClick={() => saveItems.mutate(cart)}
              loading={saveItems.isPending}
              disabled={!order.data || !dirty}
            >
              💾 {tc("save")}
            </Button>
            <Button
              variant="accent"
              onClick={saveThenPay}
              disabled={!order.data || cart.length === 0}
              loading={saveItems.isPending}
            >
              💵 {t("pay")}
            </Button>
            <Button
              variant="danger"
              className="col-span-2"
              onClick={() => cancelOrder.mutate()}
              loading={cancelOrder.isPending}
              disabled={!order.data}
            >
              {t("cancelBill")}
            </Button>
          </div>
        </div>
      </div>

      {/* ---- bottom bar ---- */}
      <div className="flex shrink-0 items-center justify-between bg-navy-800 px-3.5 py-1.5 text-xs text-white/85">
        <span>{t("cashier")}: {user?.fullName ?? "—"}</span>
        <Clock mode="full" />
      </div>
    </div>
  );
}

export default function CashierOrderPage() {
  return (
    <Suspense
      fallback={
        <div className="grid min-h-screen place-items-center bg-ink-100 text-sm text-ink-500">
          …
        </div>
      }
    >
      <PosScreen />
    </Suspense>
  );
}
