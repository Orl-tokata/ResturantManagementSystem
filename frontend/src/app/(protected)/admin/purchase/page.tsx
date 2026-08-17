"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Trash2 } from "lucide-react";
import {
  Alert,
  Badge,
  Button,
  ConfirmDialog,
  DataTable,
  Field,
  FieldRow,
  Input,
  Modal,
  Pagination,
  SearchBar,
  Select,
  StatGrid,
  StatTile,
  Textarea,
  Toolbar,
  type BadgeTone,
  type Column,
} from "@/components/ui";
import { useList } from "@/hooks/useCrud";
import { del, get, post, type PageResponse } from "@/lib/api";
import { errorMessage } from "@/lib/errors";
import { formatUsd } from "@/lib/format";
import {
  PURCHASE_STATUS_LABEL,
  type Purchase,
  type PurchaseDraftLine,
  type PurchaseStatus,
  type PurchaseSummary,
  type StockItem,
  type Supplier,
} from "@/types/supply";

const SIZE = 20;

const STATUS_TONE: Record<PurchaseStatus, BadgeTone> = {
  PENDING: "warn",
  RECEIVED: "ok",
  CANCELLED: "dead",
};

function todayIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export default function PurchasePage() {
  const qc = useQueryClient();

  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<PurchaseStatus | "">("");
  const [page, setPage] = useState(0);
  const [listError, setListError] = useState<string | null>(null);

  const [creating, setCreating] = useState(false);
  const [supplierId, setSupplierId] = useState<number | "">("");
  const [date, setDate] = useState(todayIso());
  const [note, setNote] = useState("");
  const [lines, setLines] = useState<PurchaseDraftLine[]>([]);
  const [pickItemId, setPickItemId] = useState<number | "">("");
  const [formError, setFormError] = useState<string | null>(null);

  const [viewing, setViewing] = useState<Purchase | null>(null);
  const [confirmReceive, setConfirmReceive] = useState<Purchase | null>(null);
  const [confirmCancel, setConfirmCancel] = useState<Purchase | null>(null);

  const list = useList<Purchase>("purchases", {
    search,
    status: status === "" ? undefined : status,
    page,
    size: SIZE,
  });

  const summary = useQuery({
    queryKey: ["purchases", "summary"],
    queryFn: () => get<PurchaseSummary>("/purchases/summary"),
  });

  const suppliers = useQuery({
    queryKey: ["suppliers", "active"],
    queryFn: () => get<Supplier[]>("/suppliers/active"),
    staleTime: 5 * 60_000,
  });

  const stockItems = useQuery({
    queryKey: ["stock", { size: 200 }],
    queryFn: () => get<PageResponse<StockItem>>("/stock", { size: 200 }),
    staleTime: 5 * 60_000,
  });

  function invalidate() {
    void qc.invalidateQueries({ queryKey: ["purchases"] });
    void qc.invalidateQueries({ queryKey: ["stock"] });
    void qc.invalidateQueries({ queryKey: ["suppliers"] });
  }

  const create = useMutation({
    mutationFn: () =>
      post<Purchase>("/purchases", {
        supplierId,
        purchaseDate: date,
        note: note || undefined,
        items: lines.map((l) => ({
          stockItemId: l.stockItemId,
          qty: l.qty,
          unitCost: l.unitCost,
        })),
      }),
    onSuccess: () => {
      invalidate();
      setCreating(false);
    },
    onError: (e) => setFormError(errorMessage(e, "Could not raise the purchase order")),
  });

  const receive = useMutation({
    mutationFn: (id: number) => post<Purchase>(`/purchases/${id}/receive`),
    onSuccess: () => {
      invalidate();
      setConfirmReceive(null);
    },
    onError: (e) => {
      setListError(errorMessage(e, "Could not receive the goods"));
      setConfirmReceive(null);
    },
  });

  const cancel = useMutation({
    mutationFn: (id: number) => post<Purchase>(`/purchases/${id}/cancel`),
    onSuccess: () => {
      invalidate();
      setConfirmCancel(null);
    },
    onError: (e) => {
      setListError(errorMessage(e, "Could not cancel the order"));
      setConfirmCancel(null);
    },
  });

  const removeOrder = useMutation({
    mutationFn: (id: number) => del<void>(`/purchases/${id}`),
    onSuccess: invalidate,
    onError: (e) => setListError(errorMessage(e, "Could not delete the order")),
  });

  /* ---- draft lines ---- */

  function openNew() {
    setCreating(true);
    setSupplierId(suppliers.data?.[0]?.id ?? "");
    setDate(todayIso());
    setNote("");
    setLines([]);
    setPickItemId("");
    setFormError(null);
  }

  function addLine() {
    if (pickItemId === "") return;
    const item = stockItems.data?.content.find((s) => s.id === pickItemId);
    if (!item) return;
    if (lines.some((l) => l.stockItemId === item.id)) {
      setFormError("ទំនិញនេះមានក្នុងបញ្ជីរួចហើយ · That item is already on the order");
      return;
    }
    setFormError(null);
    setLines((ls) => [
      ...ls,
      {
        stockItemId: item.id,
        itemName: item.name,
        unit: item.unit,
        qty: 1,
        // Pre-filled from the last known cost; the buyer can override it.
        unitCost: item.unitCost,
      },
    ]);
    setPickItemId("");
  }

  function updateLine(id: number, patch: Partial<PurchaseDraftLine>) {
    setLines((ls) => ls.map((l) => (l.stockItemId === id ? { ...l, ...patch } : l)));
  }

  const draftTotal = lines.reduce((sum, l) => sum + l.qty * l.unitCost, 0);

  function submit() {
    if (supplierId === "") {
      setFormError("សូមជ្រើសរើសអ្នកផ្គត់ផ្គង់ · Choose a supplier");
      return;
    }
    if (lines.length === 0) {
      setFormError("សូមបន្ថែមទំនិញយ៉ាងតិចមួយ · Add at least one item");
      return;
    }
    if (lines.some((l) => l.qty <= 0)) {
      setFormError("ចំនួនត្រូវធំជាងសូន្យ · Every quantity must be greater than zero");
      return;
    }
    create.mutate();
  }

  /* ---- columns ---- */

  const columns: Column<Purchase>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * SIZE + i + 1 },
    { key: "po", header: "លេខវិក្កយបត្រ · PO", render: (r) => <span className="font-medium">{r.poNo}</span> },
    { key: "supplier", header: "អ្នកផ្គត់ផ្គង់ · Supplier", render: (r) => r.supplierName ?? "—" },
    { key: "date", header: "កាលបរិច្ឆេទ", hideOnMobile: true, render: (r) => r.purchaseDate },
    { key: "lines", header: "ចំនួនមុខ", numeric: true, render: (r) => r.items.length },
    { key: "total", header: "សរុប · Total", numeric: true, render: (r) => formatUsd(r.total) },
    {
      key: "status",
      header: "ស្ថានភាព",
      render: (r) => <Badge tone={STATUS_TONE[r.status]}>{PURCHASE_STATUS_LABEL[r.status]}</Badge>,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (r) => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="ghost" onClick={() => setViewing(r)} aria-label="View">
            👁️
          </Button>
          {r.status === "PENDING" && (
            <>
              <Button size="sm" variant="admin" onClick={() => setConfirmReceive(r)}>
                📥 ទទួល
              </Button>
              <Button size="sm" variant="light" onClick={() => setConfirmCancel(r)}>
                បោះបង់
              </Button>
            </>
          )}
          {r.status !== "RECEIVED" && (
            <Button
              size="sm"
              variant="danger"
              onClick={() => removeOrder.mutate(r.id)}
              aria-label={`Delete ${r.poNo}`}
            >
              <Trash2 size={13} />
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <>
      {listError && <Alert tone="error">{listError}</Alert>}
      {list.isError && <Alert tone="error">{errorMessage(list.error)}</Alert>}

      <StatGrid>
        <StatTile tone={1} label="ការទិញខែនេះ · This month" value={formatUsd(summary.data?.monthTotal ?? 0)} />
        <StatTile tone={3} label="វិក្កយបត្រ · Orders" value={summary.data?.orderCount ?? "—"} />
        <StatTile tone={2} label="រង់ចាំទទួល · Pending" value={summary.data?.pendingCount ?? "—"} />
        <StatTile tone={4} label="ជំពាក់សរុប · Payable" value={formatUsd(summary.data?.payable ?? 0)} />
      </StatGrid>

      <Toolbar
        left={
          <>
            <Button variant="admin" onClick={openNew}>
              ➕ ការទិញថ្មី · New purchase
            </Button>
            <Select
              className="w-auto"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value as PurchaseStatus | "");
                setPage(0);
              }}
              aria-label="Status"
            >
              <option value="">ស្ថានភាពទាំងអស់ · All status</option>
              <option value="PENDING">{PURCHASE_STATUS_LABEL.PENDING}</option>
              <option value="RECEIVED">{PURCHASE_STATUS_LABEL.RECEIVED}</option>
              <option value="CANCELLED">{PURCHASE_STATUS_LABEL.CANCELLED}</option>
            </Select>
          </>
        }
        right={
          <SearchBar
            value={search}
            onChange={(v) => {
              setSearch(v);
              setPage(0);
            }}
            placeholder="លេខវិក្កយបត្រ · PO number"
          />
        }
      />

      <DataTable
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage="គ្មានវិក្កយបត្រទិញចូល · No purchase orders yet"
      />

      {list.data && (
        <Pagination
          page={list.data.page}
          totalPages={list.data.totalPages}
          totalElements={list.data.totalElements}
          size={list.data.size}
          onPage={setPage}
        />
      )}

      {/* ---- new purchase order ---- */}
      <Modal
        open={creating}
        onClose={() => setCreating(false)}
        title="វិក្កយបត្រទិញចូល · Purchase Order"
        width="lg"
        footer={
          <>
            <Button variant="light" onClick={() => setCreating(false)}>
              បិទ · Close
            </Button>
            <Button variant="admin" onClick={submit} loading={create.isPending}>
              រក្សាទុក · Save as pending
            </Button>
          </>
        }
      >
        {formError && <Alert tone="error">{formError}</Alert>}

        <FieldRow>
          <Field label="អ្នកផ្គត់ផ្គង់ · Supplier" htmlFor="p-supplier" required>
            <Select
              id="p-supplier"
              value={supplierId}
              onChange={(e) => setSupplierId(e.target.value === "" ? "" : Number(e.target.value))}
            >
              <option value="" disabled>
                — ជ្រើសរើស · Choose —
              </option>
              {suppliers.data?.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.company}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="កាលបរិច្ឆេទ · Date" htmlFor="p-date" required>
            <Input id="p-date" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
          </Field>
        </FieldRow>

        <div className="mb-2 flex flex-wrap items-end gap-2">
          <div className="min-w-52 flex-1">
            <Field label="បន្ថែមទំនិញ · Add item" htmlFor="p-pick">
              <Select
                id="p-pick"
                value={pickItemId}
                onChange={(e) => setPickItemId(e.target.value === "" ? "" : Number(e.target.value))}
              >
                <option value="">— ជ្រើសរើសទំនិញ —</option>
                {stockItems.data?.content.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.unit})
                  </option>
                ))}
              </Select>
            </Field>
          </div>
          <div className="mb-3.5">
            <Button variant="light" onClick={addLine} disabled={pickItemId === ""}>
              ➕ បន្ថែម
            </Button>
          </div>
        </div>

        <div className="mb-3 overflow-x-auto rounded-md border border-ink-200">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-ink-100 text-xs uppercase text-ink-500">
                <th className="px-3 py-2 text-left">ទំនិញ · Item</th>
                <th className="px-3 py-2 text-right">ចំនួន</th>
                <th className="px-3 py-2 text-right">ថ្លៃដើម</th>
                <th className="px-3 py-2 text-right">សរុប</th>
                <th className="w-10" />
              </tr>
            </thead>
            <tbody>
              {lines.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-3 py-6 text-center text-ink-500">
                    មិនទាន់មានទំនិញ · No items yet
                  </td>
                </tr>
              )}
              {lines.map((l) => (
                <tr key={l.stockItemId} className="border-t border-ink-200">
                  <td className="px-3 py-2">
                    {l.itemName} <span className="text-ink-500">({l.unit})</span>
                  </td>
                  <td className="px-2 py-1.5 text-right">
                    <Input
                      type="number"
                      step="0.01"
                      min="0.01"
                      value={l.qty}
                      onChange={(e) => updateLine(l.stockItemId, { qty: Number(e.target.value) })}
                      className="w-24 text-right"
                      aria-label={`Quantity for ${l.itemName}`}
                    />
                  </td>
                  <td className="px-2 py-1.5 text-right">
                    <Input
                      type="number"
                      step="0.01"
                      min="0"
                      value={l.unitCost}
                      onChange={(e) => updateLine(l.stockItemId, { unitCost: Number(e.target.value) })}
                      className="w-24 text-right"
                      aria-label={`Unit cost for ${l.itemName}`}
                    />
                  </td>
                  <td className="px-3 py-2 text-right font-[family-name:var(--font-num)]">
                    {formatUsd(l.qty * l.unitCost)}
                  </td>
                  <td className="pr-2 text-right">
                    <button
                      type="button"
                      onClick={() => setLines((ls) => ls.filter((x) => x.stockItemId !== l.stockItemId))}
                      className="text-danger-soft"
                      aria-label={`Remove ${l.itemName}`}
                    >
                      <Trash2 size={14} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="mb-3 flex justify-between text-lg font-bold">
          <span>សរុប · Total</span>
          <span className="font-[family-name:var(--font-num)]">{formatUsd(draftTotal)}</span>
        </div>

        <Field label="ចំណាំ · Note" htmlFor="p-note">
          <Textarea id="p-note" value={note} onChange={(e) => setNote(e.target.value)} />
        </Field>

        <p className="text-xs text-ink-500">
          Saved as <b>PENDING</b>. Stock only changes when you press Receive.
        </p>
      </Modal>

      {/* ---- view ---- */}
      <Modal
        open={viewing !== null}
        onClose={() => setViewing(null)}
        title={`${viewing?.poNo ?? ""} · ${viewing?.supplierName ?? ""}`}
        width="lg"
        footer={
          <Button variant="light" onClick={() => setViewing(null)}>
            បិទ · Close
          </Button>
        }
      >
        <div className="mb-3 flex flex-wrap gap-4 text-sm">
          <span>
            កាលបរិច្ឆេទ · <b>{viewing?.purchaseDate}</b>
          </span>
          <span>
            ស្ថានភាព ·{" "}
            {viewing && (
              <Badge tone={STATUS_TONE[viewing.status]}>{PURCHASE_STATUS_LABEL[viewing.status]}</Badge>
            )}
          </span>
        </div>

        <div className="overflow-x-auto rounded-md border border-ink-200">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-ink-100 text-xs uppercase text-ink-500">
                <th className="px-3 py-2 text-left">ទំនិញ</th>
                <th className="px-3 py-2 text-right">ចំនួន</th>
                <th className="px-3 py-2 text-right">ថ្លៃដើម</th>
                <th className="px-3 py-2 text-right">សរុប</th>
              </tr>
            </thead>
            <tbody>
              {viewing?.items.map((i, idx) => (
                <tr key={i.id ?? idx} className="border-t border-ink-200">
                  <td className="px-3 py-2">{i.itemName}</td>
                  <td className="px-3 py-2 text-right">{i.qty}</td>
                  <td className="px-3 py-2 text-right">{formatUsd(i.unitCost)}</td>
                  <td className="px-3 py-2 text-right">{formatUsd(i.lineTotal)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="mt-3 flex justify-between text-lg font-bold">
          <span>សរុប · Total</span>
          <span className="font-[family-name:var(--font-num)]">{formatUsd(viewing?.total ?? 0)}</span>
        </div>

        {viewing?.note && <p className="mt-2 text-sm text-ink-500">{viewing.note}</p>}
      </Modal>

      <ConfirmDialog
        open={confirmReceive !== null}
        busy={receive.isPending}
        destructive={false}
        onClose={() => setConfirmReceive(null)}
        onConfirm={() => confirmReceive && receive.mutate(confirmReceive.id)}
        title="ទទួលទំនិញ · Receive goods"
        message={`ទទួលទំនិញពី ${confirmReceive?.poNo ?? ""} មែនទេ?`}
        detail={`This adds every line to stock, records a movement for each, and puts ${formatUsd(
          confirmReceive?.total ?? 0,
        )} on the supplier's payable. It cannot be undone.`}
        confirmLabel="បាទ/ចាស ទទួល · Yes, receive"
      />

      <ConfirmDialog
        open={confirmCancel !== null}
        busy={cancel.isPending}
        onClose={() => setConfirmCancel(null)}
        onConfirm={() => confirmCancel && cancel.mutate(confirmCancel.id)}
        title="បោះបង់វិក្កយបត្រ · Cancel order"
        message={`បោះបង់ ${confirmCancel?.poNo ?? ""} មែនទេ?`}
        detail="Only a pending order can be cancelled."
        confirmLabel="បាទ/ចាស បោះបង់ · Yes, cancel"
      />
    </>
  );
}
