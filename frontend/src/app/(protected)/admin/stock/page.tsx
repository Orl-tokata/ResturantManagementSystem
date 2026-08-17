"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Badge,
  Button,
  ConfirmDialog,
  DataTable,
  Field,
  FieldRow,
  Input,
  Meter,
  Modal,
  Pagination,
  SearchBar,
  Select,
  StatGrid,
  StatTile,
  Textarea,
  Toolbar,
  type Column,
} from "@/components/ui";
import { useList, useRemove, useSave } from "@/hooks/useCrud";
import { get, post, type PageResponse } from "@/lib/api";
import { errorMessage } from "@/lib/errors";
import { formatUsd } from "@/lib/format";
import {
  MOVEMENT_LABEL,
  type AdjustRequest,
  type Movement,
  type MovementType,
  type StockItem,
  type StockItemRequest,
  type StockSummary,
} from "@/types/supply";

const EMPTY: StockItemRequest = { name: "", unit: "", qty: 0, minQty: 0, unitCost: 0 };
const EMPTY_ADJUST: AdjustRequest = { type: "IN", qty: 1, reason: "" };
const SIZE = 20;

export default function StockPage() {
  const qc = useQueryClient();

  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [onlyLow, setOnlyLow] = useState(false);

  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  const [draft, setDraft] = useState<StockItemRequest>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);

  const [adjusting, setAdjusting] = useState<StockItem | null>(null);
  const [adjustDraft, setAdjustDraft] = useState<AdjustRequest>(EMPTY_ADJUST);
  const [adjustError, setAdjustError] = useState<string | null>(null);

  const [historyFor, setHistoryFor] = useState<StockItem | null>(null);
  const [deleting, setDeleting] = useState<StockItem | null>(null);
  const [listError, setListError] = useState<string | null>(null);

  const list = useList<StockItem>("stock", { search, page, size: SIZE });
  const save = useSave<StockItem, StockItemRequest>("stock");
  const remove = useRemove("stock");

  const summary = useQuery({
    queryKey: ["stock", "summary"],
    queryFn: () => get<StockSummary>("/stock/summary"),
  });

  const movements = useQuery({
    queryKey: ["stock", historyFor?.id, "movements"],
    queryFn: () => get<PageResponse<Movement>>(`/stock/${historyFor!.id}/movements`, { size: 50 }),
    enabled: historyFor !== null,
  });

  const adjust = useMutation({
    mutationFn: () => post<StockItem>(`/stock/${adjusting!.id}/adjust`, adjustDraft),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["stock"] });
      setAdjusting(null);
    },
    onError: (e) => setAdjustError(errorMessage(e, "Could not adjust the stock")),
  });

  const rows = (list.data?.content ?? []).filter((r) => !onlyLow || r.lowStock);

  function set<K extends keyof StockItemRequest>(key: K, value: StockItemRequest[K]) {
    setDraft((d) => ({ ...d, [key]: value }));
  }

  function openNew() {
    setEditingId(null);
    setDraft(EMPTY);
    setFormError(null);
  }

  function openEdit(r: StockItem) {
    setEditingId(r.id);
    setDraft({ name: r.name, unit: r.unit, qty: r.qty, minQty: r.minQty, unitCost: r.unitCost });
    setFormError(null);
  }

  async function submit() {
    if (!draft.name.trim() || !draft.unit.trim()) {
      setFormError("ឈ្មោះ និងឯកតាត្រូវការ · Name and unit are required");
      return;
    }
    try {
      await save.mutateAsync({ id: editingId ?? null, body: draft });
      setEditingId(undefined);
      void qc.invalidateQueries({ queryKey: ["stock", "summary"] });
    } catch (e) {
      setFormError(errorMessage(e, "Could not save the stock item"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      setListError(errorMessage(e, "Could not delete the stock item"));
    } finally {
      setDeleting(null);
    }
  }

  const columns: Column<StockItem>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * SIZE + i + 1 },
    { key: "name", header: "ទំនិញ · Item", render: (r) => <span className="font-medium">{r.name}</span> },
    { key: "unit", header: "ឯកតា", hideOnMobile: true, render: (r) => r.unit },
    { key: "qty", header: "ស្តុក", numeric: true, render: (r) => r.qty },
    { key: "min", header: "អប្បបរមា", numeric: true, hideOnMobile: true, render: (r) => r.minQty },
    {
      key: "level",
      header: "កម្រិត · Level",
      width: "130px",
      render: (r) => <Meter value={r.qty} max={r.minQty} />,
    },
    { key: "cost", header: "ថ្លៃដើម", numeric: true, hideOnMobile: true, render: (r) => formatUsd(r.unitCost) },
    { key: "value", header: "តម្លៃ", numeric: true, hideOnMobile: true, render: (r) => formatUsd(r.value) },
    {
      key: "status",
      header: "ស្ថានភាព",
      render: (r) =>
        r.outOfStock ? (
          <Badge tone="dead">អស់ស្តុក</Badge>
        ) : r.lowStock ? (
          <Badge tone="warn">ជិតអស់</Badge>
        ) : (
          <Badge tone="ok">គ្រប់គ្រាន់</Badge>
        ),
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (r) => (
        <div className="flex justify-end gap-1.5">
          <Button
            size="sm"
            variant="admin"
            onClick={() => {
              setAdjusting(r);
              setAdjustDraft(EMPTY_ADJUST);
              setAdjustError(null);
            }}
          >
            📦 កែតម្រូវ
          </Button>
          <Button size="sm" variant="ghost" onClick={() => setHistoryFor(r)} aria-label="History">
            🕘
          </Button>
          <Button size="sm" variant="ghost" onClick={() => openEdit(r)} aria-label={`Edit ${r.name}`}>
            ✏️
          </Button>
          <Button size="sm" variant="danger" onClick={() => setDeleting(r)} aria-label={`Delete ${r.name}`}>
            🗑️
          </Button>
        </div>
      ),
    },
  ];

  const movementColumns: Column<Movement>[] = [
    {
      key: "when",
      header: "កាលបរិច្ឆេទ",
      render: (m) => new Date(m.createdAt).toLocaleString(),
    },
    {
      key: "type",
      header: "ប្រភេទ",
      render: (m) => (
        <Badge tone={m.type === "IN" ? "ok" : m.type === "OUT" ? "info" : "dead"}>
          {MOVEMENT_LABEL[m.type]}
        </Badge>
      ),
    },
    { key: "qty", header: "ចំនួន", numeric: true, render: (m) => m.qty },
    { key: "by", header: "អ្នកធ្វើ", render: (m) => m.createdBy ?? "—" },
    { key: "why", header: "មូលហេតុ", render: (m) => m.reason ?? "—" },
  ];

  return (
    <>
      {listError && <Alert tone="error">{listError}</Alert>}
      {list.isError && <Alert tone="error">{errorMessage(list.error)}</Alert>}

      <StatGrid>
        <StatTile tone={1} label="ទំនិញសរុប · Total items" value={summary.data?.totalItems ?? "—"} />
        <StatTile tone={3} label="តម្លៃស្តុក · Stock value" value={formatUsd(summary.data?.stockValue ?? 0)} />
        <StatTile tone={2} label="ជិតអស់ · Low stock" value={summary.data?.lowStockCount ?? "—"} />
        <StatTile tone={4} label="អស់ស្តុក · Out of stock" value={summary.data?.outOfStockCount ?? "—"} />
      </StatGrid>

      <Toolbar
        left={
          <>
            <Button variant="admin" onClick={openNew}>
              ➕ បន្ថែមទំនិញ · Add item
            </Button>
            <Select
              className="w-auto"
              value={onlyLow ? "low" : "all"}
              onChange={(e) => setOnlyLow(e.target.value === "low")}
              aria-label="Filter"
            >
              <option value="all">ទាំងអស់ · All</option>
              <option value="low">ជិតអស់ · Low stock only</option>
            </Select>
          </>
        }
        right={<SearchBar value={search} onChange={(v) => { setSearch(v); setPage(0); }} />}
      />

      <DataTable
        columns={columns}
        rows={rows}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage="គ្មានទំនិញ · No stock items found"
      />

      {list.data && !onlyLow && (
        <Pagination
          page={list.data.page}
          totalPages={list.data.totalPages}
          totalElements={list.data.totalElements}
          size={list.data.size}
          onPage={setPage}
        />
      )}

      {/* ---- create / edit ---- */}
      <Modal
        open={editingId !== undefined}
        onClose={() => setEditingId(undefined)}
        title="ព័ត៌មានទំនិញ · Stock Item"
        width="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setEditingId(undefined)}>
              បិទ · Close
            </Button>
            <Button variant="admin" onClick={submit} loading={save.isPending}>
              រក្សាទុក · Save
            </Button>
          </>
        }
      >
        {formError && <Alert tone="error">{formError}</Alert>}

        <FieldRow>
          <Field label="ឈ្មោះ · Name" htmlFor="k-name" required>
            <Input id="k-name" value={draft.name} onChange={(e) => set("name", e.target.value)} placeholder="សាច់គោ" />
          </Field>
          <Field label="ឯកតា · Unit" htmlFor="k-unit" required>
            <Input id="k-unit" value={draft.unit} onChange={(e) => set("unit", e.target.value)} placeholder="គីឡូក្រាម" />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field
            label="ស្តុក · Quantity"
            htmlFor="k-qty"
            hint={editingId !== null ? "Editing this directly leaves no audit trail — prefer Adjust" : undefined}
          >
            <Input
              id="k-qty"
              type="number"
              step="0.01"
              min="0"
              value={draft.qty ?? 0}
              onChange={(e) => set("qty", Number(e.target.value))}
            />
          </Field>
          <Field label="កម្រិតអប្បបរមា · Minimum" htmlFor="k-min">
            <Input
              id="k-min"
              type="number"
              step="0.01"
              min="0"
              value={draft.minQty ?? 0}
              onChange={(e) => set("minQty", Number(e.target.value))}
            />
          </Field>
        </FieldRow>

        <Field label="ថ្លៃដើមក្នុងឯកតា · Unit cost ($)" htmlFor="k-cost">
          <Input
            id="k-cost"
            type="number"
            step="0.01"
            min="0"
            value={draft.unitCost ?? 0}
            onChange={(e) => set("unitCost", Number(e.target.value))}
          />
        </Field>
      </Modal>

      {/* ---- adjust ---- */}
      <Modal
        open={adjusting !== null}
        onClose={() => setAdjusting(null)}
        title={`កែតម្រូវស្តុក · Adjust — ${adjusting?.name ?? ""}`}
        width="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setAdjusting(null)}>
              បិទ · Close
            </Button>
            <Button variant="admin" onClick={() => adjust.mutate()} loading={adjust.isPending}>
              រក្សាទុក · Save
            </Button>
          </>
        }
      >
        {adjustError && <Alert tone="error">{adjustError}</Alert>}

        <p className="mb-3 text-sm text-ink-500">
          ស្តុកបច្ចុប្បន្ន · Currently{" "}
          <b className="text-ink-900">
            {adjusting?.qty} {adjusting?.unit}
          </b>
        </p>

        <FieldRow>
          <Field label="ប្រភេទ · Type" htmlFor="a-type" required>
            <Select
              id="a-type"
              value={adjustDraft.type}
              onChange={(e) =>
                setAdjustDraft({ ...adjustDraft, type: e.target.value as MovementType })
              }
            >
              <option value="IN">{MOVEMENT_LABEL.IN}</option>
              <option value="OUT">{MOVEMENT_LABEL.OUT}</option>
              <option value="DAMAGED">{MOVEMENT_LABEL.DAMAGED}</option>
            </Select>
          </Field>
          <Field
            label="ចំនួន · Quantity"
            htmlFor="a-qty"
            required
            hint="Always positive — the type decides the direction"
          >
            <Input
              id="a-qty"
              type="number"
              step="0.01"
              min="0.01"
              value={adjustDraft.qty}
              onChange={(e) => setAdjustDraft({ ...adjustDraft, qty: Number(e.target.value) })}
            />
          </Field>
        </FieldRow>

        <Field label="មូលហេតុ · Reason" htmlFor="a-why">
          <Textarea
            id="a-why"
            value={adjustDraft.reason ?? ""}
            onChange={(e) => setAdjustDraft({ ...adjustDraft, reason: e.target.value })}
            placeholder="ទិញបន្ថែម / ខូច / រាប់ឡើងវិញ"
          />
        </Field>
      </Modal>

      {/* ---- movement history ---- */}
      <Modal
        open={historyFor !== null}
        onClose={() => setHistoryFor(null)}
        title={`ប្រវត្តិស្តុក · Movements — ${historyFor?.name ?? ""}`}
        width="lg"
        footer={
          <Button variant="light" onClick={() => setHistoryFor(null)}>
            បិទ · Close
          </Button>
        }
      >
        <DataTable
          columns={movementColumns}
          rows={movements.data?.content ?? []}
          rowKey={(m) => m.id}
          loading={movements.isLoading}
          emptyMessage="គ្មានចលនាស្តុក · No movements recorded yet"
        />
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        message={`តើអ្នកប្រាកដជាចង់លុប "${deleting?.name ?? ""}" មែនទេ?`}
        detail="An item with movement history cannot be deleted — set its quantity to zero instead."
      />
    </>
  );
}
