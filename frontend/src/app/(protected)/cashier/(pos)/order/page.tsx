"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Minus, Plus, Trash2, X } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { Clock } from "@/components/layout/Clock";
import { ShiftGate } from "@/components/shift/ShiftGate";
import { Alert, Button, SearchBar } from "@/components/ui";
import { CustomerChip } from "@/components/pos/CustomerChip";
import { LineChooser } from "@/components/pos/LineChooser";
import { PaymentPanel } from "@/components/pos/PaymentPanel";
import { get, post, put, type PageResponse } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import { httpStatus } from "@/lib/errors";
import { pickName } from "@/i18n/name";
import { formatKhr, formatUsd } from "@/lib/format";
import { useAuth } from "@/lib/auth-context";
import type { Category, DiningTable, Product } from "@/types/master";
import { cartLineKey, type CartLine, type Order } from "@/types/order";
import type { Variant } from "@/types/catalog-extras";
import { ProductImage } from "@/components/ui/ProductImage";

function PosScreen() {
  const params = useSearchParams();
  const router = useRouter();
  const qc = useQueryClient();
  const { user } = useAuth();

  const tableId = Number(params.get("tableId") || 0);

  const t = useTranslations("pos");
  const tc = useTranslations("common");
  const tA11y = useTranslations("a11y");
  const apiError = useApiError();
  const locale = useLocale();

  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [search, setSearch] = useState("");
  const [cart, setCart] = useState<CartLine[]>([]);
  /** The dish whose sizes and questions are being asked about, if any. */
  const [choosing, setChoosing] = useState<Product | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [dirty, setDirty] = useState(false);
  /* Settling happens here rather than on a screen of its own — SCREENS.md 2.1. */
  const [paying, setPaying] = useState(false);

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

  /*
   * Reads the bill for this table. Deliberately a GET.
   *
   * It used to POST /orders here, which meant loading the screen created a
   * bill — burning an invoice number and marking the table occupied before a
   * single dish was tapped. Open the POS, change your mind, walk away, and an
   * empty bill sat on that table for the rest of the day. Five of the last
   * twelve orders in the real database were created that way and contained
   * nothing.
   *
   * A 404 is the ordinary answer here: this table has no bill yet.
   */
  const order = useQuery({
    queryKey: ["order", "table", tableId],
    queryFn: async () => {
      try {
        return await get<Order>("/orders/open", { tableId });
      } catch (e) {
        if (httpStatus(e) === 404) return null;
        throw e;
      }
    },
    enabled: tableId > 0,
    staleTime: Infinity,
    retry: false,
  });

  const bill = order.data ?? null;

  /*
   * The table's name, which until a bill exists has to come from somewhere
   * else. Cheap, cached, and better than showing "Table 1" where every other
   * screen says "Table 01".
   */
  const table = useQuery({
    queryKey: ["table", tableId],
    queryFn: () => get<DiningTable>(`/tables/${tableId}`),
    enabled: tableId > 0,
    staleTime: 5 * 60_000,
  });

  /**
   * Creates the bill if this table has none yet, and returns it either way.
   *
   * <p>Called from saving and from paying — the two moments something real
   * happens. POST /orders is idempotent per table on the server, so a racing
   * second call returns the same bill rather than a second one.
   */
  const openBill = useMutation({
    mutationFn: () => post<Order>("/orders", { tableId }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["tables"] });
      /*
       * Deliberately not written into ["order", "table", …].
       *
       * A bill created a moment ago has no items, and publishing it made the
       * seeding block below see a new id and reset the basket to empty —
       * losing what the cashier had just tapped, while the save that followed
       * quietly succeeded. The screen showed nothing for a bill that had one
       * line on the server.
       *
       * ensureBill hands the id straight to the caller, so nothing needs it in
       * the cache. The save's own onSuccess publishes the bill once it has
       * contents.
       */
    },
  });

  async function ensureBill(): Promise<Order> {
    return bill ?? (await openBill.mutateAsync());
  }

  // Seed the basket from the server exactly once per bill; after that the local
  // cart is the source of truth until it is saved. Adjusted during render
  // rather than in an effect, so the first paint already shows the lines.
  // `dirty` guards it as well as the id: a server write arriving mid-edit must
  // not overwrite what the cashier is still typing, whichever bill it is for.
  const [seededFor, setSeededFor] = useState<number | null>(null);
  if (bill && seededFor !== bill.id && !dirty) {
    setSeededFor(bill.id);
    setCart(
      bill.items.map((i) => ({
        key: cartLineKey(
          i.productId ?? 0,
          i.variantId,
          (i.modifiers ?? []).map((m) => m.modifierId ?? 0),
        ),
        productId: i.productId ?? 0,
        productName: i.productName,
        variantId: i.variantId,
        variantName: i.variantName,
        modifierIds: (i.modifiers ?? []).map((m) => m.modifierId ?? 0),
        modifierNames: (i.modifiers ?? []).map((m) => m.name),
        unitPrice: i.unitPrice,
        qty: i.qty,
        note: i.note ?? undefined,
      })),
    );
    setDirty(false);
  }

  /* ---- Mutations ------------------------------------------------------ */

  const saveItems = useMutation({
    // The id is passed in rather than read from the query, because the bill may
    // have been created moments earlier by ensureBill and the cache write that
    // follows it has not necessarily landed yet.
    mutationFn: ({ orderId, lines }: { orderId: number; lines: CartLine[] }) =>
      put<Order>(`/orders/${orderId}/items`, {
        items: lines.map((l) => ({
          productId: l.productId,
          qty: l.qty,
          variantId: l.variantId,
          // Ids only. The server reads every price from the menu; one that
          // arrived from here would be a price the till chose.
          modifierIds: l.modifierIds,
          note: l.note,
        })),
      }),
    onSuccess: (updated) => {
      qc.setQueryData(["order", "table", tableId], updated);
      setDirty(false);
    },
  });

  const cancelOrder = useMutation({
    mutationFn: () => post<Order>(`/orders/${bill!.id}/cancel`),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["tables"] });
      router.push("/cashier/tables");
    },
  });

  /**
   * Names the customer on the bill, creating it first if the cashier asked
   * before anything was ordered.
   *
   * <p>The response is written straight into the cache rather than
   * invalidated: a refetch while the basket is dirty is the shape of the bug
   * that once wiped a cashier's taps, and the seeding block below is guarded
   * against exactly this.
   */
  async function setCustomer(customerId: number | null) {
    const b = await ensureBill();
    const updated = await put<Order>(`/orders/${b.id}/customer`, { customerId });
    qc.setQueryData(["order", "table", tableId], updated);
  }

  /** Saves, creating the bill first if this is the first thing on it. */
  async function save() {
    setError(null);
    try {
      const b = await ensureBill();
      await saveItems.mutateAsync({ orderId: b.id, lines: cart });
    } catch (e) {
      setError(apiError(e, "saveOrder"));
    }
  }

  /**
   * Leaving without ordering anything.
   *
   * <p>With no bill there is nothing to cancel — and nothing was ever created,
   * which is the point of the change. Just go back to the tables.
   */
  function discard() {
    if (!bill) {
      router.push("/cashier/tables");
      return;
    }
    cancelOrder.mutate();
  }

  /* ---- Cart operations ------------------------------------------------ */

  /**
   * A tap on a tile.
   *
   * <p>Straight into the basket for the ordinary dish, and into the chooser
   * for one that has sizes or questions. The flag comes down with the product
   * so the common case costs no round trip.
   */
  function addProduct(p: Product) {
    if (p.hasOptions) {
      setChoosing(p);
      return;
    }
    addLine({ product: p, modifiers: [], unitPrice: p.price });
  }

  function addLine({
    product,
    variant,
    modifiers,
    unitPrice,
  }: {
    product: Product;
    variant?: Variant;
    modifiers: { id: number; name: string }[];
    unitPrice: number;
  }) {
    const modifierIds = modifiers.map((m) => m.id);
    const key = cartLineKey(product.id, variant?.id, modifierIds);

    setCart((lines) => {
      const found = lines.find((l) => l.key === key);
      if (found) {
        return lines.map((l) => (l.key === key ? { ...l, qty: l.qty + 1 } : l));
      }
      return [
        ...lines,
        {
          key,
          productId: product.id,
          productName: product.name,
          variantId: variant?.id,
          variantName: variant?.name,
          modifierIds,
          modifierNames: modifiers.map((m) => m.name),
          unitPrice,
          qty: 1,
        },
      ];
    });
    setDirty(true);
    setChoosing(null);
  }

  function changeQty(key: string, delta: number) {
    setCart((lines) =>
      lines
        .map((l) => (l.key === key ? { ...l, qty: l.qty + delta } : l))
        // Decrementing to zero removes the line, which is what a cashier expects.
        .filter((l) => l.qty > 0),
    );
    setDirty(true);
  }

  function removeLine(key: string) {
    setCart((lines) => lines.filter((l) => l.key !== key));
    setDirty(true);
  }

  /* ---- Local totals ---------------------------------------------------
     Shown live while the cashier taps. The server recomputes on save and its
     numbers win — these exist so the panel is not blank between requests.  */

  const totals = useMemo(() => {
    const subtotal = cart.reduce((sum, l) => sum + l.unitPrice * l.qty, 0);
    const rate = bill?.vatRate ?? 10;
    const vat = (subtotal * rate) / 100;
    return { subtotal, vat, total: subtotal + vat, rate };
  }, [cart, bill?.vatRate]);

  /*
   * The basket has to reach the server before it can be paid for — the panel
   * settles whatever the server holds, not what is on screen. Opening the panel
   * only after a successful save is what keeps those two the same thing.
   */
  async function saveThenPay() {
    setError(null);
    try {
      const b = await ensureBill();
      // Always save before paying, not only when dirty: on a bill created two
      // lines above, nothing has reached the server yet however clean the local
      // cart looks.
      await saveItems.mutateAsync({ orderId: b.id, lines: cart });
      setPaying(true);
    } catch (e) {
      setError(apiError(e, "saveOrder"));
    }
  }

  /* ---- No table chosen -------------------------------------------------
     A bookmark, a typed URL, or a back button after the bill was settled. Send
     them to the table picker rather than showing a card with one button and no
     sidebar to leave by — it is where they were trying to get to anyway. */

  useEffect(() => {
    if (!tableId) router.replace("/cashier/tables");
  }, [tableId, router]);

  if (!tableId) {
    return (
      <div className="grid min-h-screen place-items-center bg-ink-100 text-sm text-ink-500">
        …
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-ink-200">
      {/* ---- top strip ---- */}
      <div className="flex shrink-0 items-center gap-2.5 bg-navy-800 px-3 py-2 text-white">
        <Link
          href="/cashier/tables"
          aria-label={tA11y("backToTables")}
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
            {bill?.tableName ?? table.data?.name ?? `Table ${tableId}`}
          </b>
          {/* No number until there is a bill to number. Saying so beats an
              ellipsis that never resolves. */}
          <span className="text-white/70">{bill?.invoiceNo ?? t("notStarted")}</span>
        </div>
      </div>

      {error && (
        <div className="px-3 pt-2">
          <Alert tone="error">{error}</Alert>
        </div>
      )}
      {order.isError && (
        <div className="px-3 pt-2">
          <Alert tone="error">{apiError(order.error, "openBill")}</Alert>
        </div>
      )}

      {choosing && (
        <LineChooser
          product={choosing}
          onCancel={() => setChoosing(null)}
          onAdd={({ variant, modifiers, unitPrice }) =>
            addLine({ product: choosing, variant, modifiers, unitPrice })
          }
        />
      )}

      {/* ---- body ---- */}
      <div
        className={`grid min-h-0 flex-1 grid-cols-1 ${
          paying ? "md:grid-cols-[92px_1fr_420px]" : "md:grid-cols-[92px_1fr_330px]"
        }`}
      >
        {/* category rail */}
        <div
          aria-hidden={paying}
          className={`scroll-invert hidden flex-col gap-1.5 overflow-y-auto bg-teal-800 p-1.5 md:flex ${
            paying ? "pointer-events-none opacity-40" : ""
          }`}
        >
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

        {/* product grid — inert while settling, so a dish cannot be added
            after the amount tendered has been counted out */}
        <div
          aria-hidden={paying}
          className={`grid auto-rows-min grid-cols-[repeat(auto-fill,minmax(128px,1fr))] gap-2.5 overflow-y-auto bg-ink-100 p-3 ${
            paying ? "pointer-events-none opacity-40" : ""
          }`}
        >
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
                {/* 4:3 rather than a fixed 78px. A photograph of a dish is
                    roughly square, so in a short strip it shrank to fit the
                    height and left most of the tile grey. An aspect ratio also
                    grows the picture with the column on a wider screen, which
                    a fixed height cannot. */}
                <div className="flex aspect-[4/3] items-center justify-center overflow-hidden bg-ink-200">
                  <ProductImage file={p.imageFile} icon={p.icon} alt={p.name} />
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

          {/* A floor rather than min-h-0: on a phone the payment panel would
              otherwise take the whole column and squeeze the bill to nothing,
              and keeping the bill in view is the point of settling here. One
              line and a scrollbar is little, but it is not nothing. */}
          <div className="min-h-[3.25rem] flex-1 overflow-y-auto">
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
                    <tr key={l.key} className="border-b border-dashed border-ink-300">
                      <td className="px-2 py-2">
                        <div className="font-semibold">
                          {l.productName}
                          {l.variantName && (
                            <span className="ml-1 font-normal text-ink-500">
                              ({l.variantName})
                            </span>
                          )}
                        </div>
                        {/* What was asked for, under the dish it was asked
                            about \u2014 the kitchen reads this line, not the
                            modal it was chosen in. */}
                        {l.modifierNames.length > 0 && (
                          <div className="text-[11px] text-teal-700">
                            {l.modifierNames.join(", ")}
                          </div>
                        )}
                        <div className="text-ink-500">{formatUsd(l.unitPrice)}</div>
                      </td>
                      <td className="px-1 py-2">
                        <div className="flex items-center gap-1">
                          <button
                            type="button"
                            aria-label={tA11y("decrease", { name: l.productName })}
                            onClick={() => changeQty(l.key, -1)}
                            className="grid h-6 w-6 place-items-center rounded border border-ink-300 bg-white"
                          >
                            <Minus size={12} />
                          </button>
                          <span className="w-6 text-center font-[family-name:var(--font-num)] font-bold">
                            {l.qty}
                          </span>
                          <button
                            type="button"
                            aria-label={tA11y("increase", { name: l.productName })}
                            onClick={() => changeQty(l.key, 1)}
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
                          aria-label={tA11y("remove", { name: l.productName })}
                          onClick={() => removeLine(l.key)}
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

          {/* Who the bill belongs to. Above the totals because it changes what
              the customer gets out of this meal, not what they pay for it. */}
          <CustomerChip
            customerId={bill?.customerId}
            customerName={bill?.customerName}
            onPick={setCustomer}
            disabled={paying}
          />

          {/* totals — the local estimate, for while the cashier is tapping.
              The panel below shows the server's figures instead. */}
          <div
            className={`border-t-2 border-navy-800 bg-[#f7edd8] px-3 py-2.5 text-sm ${
              paying ? "hidden" : ""
            }`}
          >
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

          {paying && bill ? (
            <PaymentPanel
              order={bill}
              onPaid={() => router.replace(`/cashier/receipt/${bill.id}`)}
              onBack={() => setPaying(false)}
            />
          ) : (
            <div className="grid grid-cols-2 gap-2 p-2.5">
              <Button
                variant="light"
                onClick={save}
                loading={saveItems.isPending || openBill.isPending}
                // Nothing to save, and nothing worth opening a bill for: an
                // empty basket is exactly the case this change exists to stop
                // from creating one.
                disabled={!dirty || (cart.length === 0 && !bill)}
              >
                💾 {tc("save")}
              </Button>
              <Button
                variant="accent"
                onClick={saveThenPay}
                disabled={cart.length === 0}
                loading={saveItems.isPending || openBill.isPending}
              >
                💵 {t("pay")}
              </Button>
              <Button
                variant="danger"
                className="col-span-2"
                onClick={discard}
                loading={cancelOrder.isPending}
              >
                {bill ? t("cancelBill") : t("leave")}
              </Button>
            </div>
          )}
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
      {/* No open shift, no POS — docs/SCREENS.md §3.1. The backend refuses the
          payment anyway; this is so the cashier learns it before the customer
          is standing there with a note in their hand. */}
      <ShiftGate>
        <PosScreen />
      </ShiftGate>
    </Suspense>
  );
}
