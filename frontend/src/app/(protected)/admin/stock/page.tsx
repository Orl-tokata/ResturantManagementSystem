"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Badge,
  Button,
  type Column,
  ConfirmDialog,
  DataTable,
  Field,
  FieldRow,
  Input,
  ListPage,
  Meter,
  Modal,
  Pagination,
  SearchBar,
  Select,
  StatGrid,
  StatTile,
  Tabs,
  Textarea,
  Toolbar,
  useToast,
} from "@/components/ui";
import { useList, useRemove, useSave } from "@/hooks/useCrud";
import { get, post, type PageResponse } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import { formatUsd } from "@/lib/format";
import {
  type AdjustRequest,
  type Movement,
  type MovementType,
  type StockItem,
  type StockItemRequest,
  type StockSummary,
} from "@/types/supply";

const EMPTY: StockItemRequest = { name: "", unit: "", qty: 0, minQty: 0, unitCost: 0 };
const EMPTY_ADJUST: AdjustRequest = { type: "IN", qty: 1, reason: "" };
/** Where this screen starts; the reader can change it. */
const INITIAL_SIZE = 20;

/** Added, removed by hand, or gone for a reason of its own. */
const MOVEMENT_TONE: Record<MovementType, "ok" | "info" | "dead"> = {
  IN: "ok",
  RETURN: "ok",
  OUT: "info",
  SALE: "info",
  DAMAGED: "dead",
};

export default function StockPage() {
  const t = useTranslations("stock");
  const tc = useTranslations("common");
  const tMv = useTranslations("enum.movementType");
  const tA11y = useTranslations("a11y");
  const apiError = useApiError();
  const toast = useToast();

  const qc = useQueryClient();

  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(INITIAL_SIZE);
  const [onlyLow, setOnlyLow] = useState(false);

  /*
   * Levels answer "how much is there"; the ledger answers "why". They are two
   * tabs of one screen because the second question only ever arrives from the
   * first, and because the ledger covers dishes as well as ingredients — it has
   * no other screen it could sensibly live on.
   */
  const [tab, setTab] = useState<"levels" | "ledger">("levels");
  const [ledgerPage, setLedgerPage] = useState(0);
  const [ledgerSize, setLedgerSize] = useState(INITIAL_SIZE);

  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  const [draft, setDraft] = useState<StockItemRequest>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);

  const [adjusting, setAdjusting] = useState<StockItem | null>(null);
  const [adjustDraft, setAdjustDraft] = useState<AdjustRequest>(EMPTY_ADJUST);
  const [adjustError, setAdjustError] = useState<string | null>(null);

  const [historyFor, setHistoryFor] = useState<StockItem | null>(null);
  const [deleting, setDeleting] = useState<StockItem | null>(null);

  const list = useList<StockItem>("stock", { search, page, size: size });
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

  const ledger = useQuery({
    queryKey: ["stock", "ledger", ledgerPage, ledgerSize],
    queryFn: () =>
      get<PageResponse<Movement>>("/stock/ledger", { page: ledgerPage, size: ledgerSize }),
    // Not fetched until the tab is opened: it is the larger query of the two
    // and most visits to this screen only want the levels.
    enabled: tab === "ledger",
  });

  const adjust = useMutation({
    mutationFn: () => post<StockItem>(`/stock/${adjusting!.id}/adjust`, adjustDraft),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ["stock"] });
      setAdjusting(null);
    },
    onError: (e) => setAdjustError(apiError(e, "adjustStock")),
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
      setFormError(t("errRequired"));
      return;
    }
    try {
      await save.mutateAsync({ id: editingId ?? null, body: draft });
      setEditingId(undefined);
      void qc.invalidateQueries({ queryKey: ["stock", "summary"] });
    } catch (e) {
      setFormError(apiError(e, "saveStockItem"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      toast.error(apiError(e, "deleteStockItem"));
    } finally {
      setDeleting(null);
    }
  }

  const columns: Column<StockItem>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * size + i + 1 },
    { key: "name", header: tc("name"), render: (r) => <span className="font-medium">{r.name}</span> },
    { key: "unit", header: tc("unit"), hideOnMobile: true, render: (r) => r.unit },
    { key: "qty", header: t("onHand"), numeric: true, render: (r) => r.qty },
    { key: "min", header: t("minQty"), numeric: true, hideOnMobile: true, render: (r) => r.minQty },
    {
      key: "level",
      header: t("level"),
      width: "130px",
      render: (r) => <Meter value={r.qty} max={r.minQty} />,
    },
    { key: "cost", header: tc("cost"), numeric: true, hideOnMobile: true, render: (r) => formatUsd(r.unitCost) },
    { key: "value", header: t("value"), numeric: true, hideOnMobile: true, render: (r) => formatUsd(r.value) },
    {
      key: "status",
      header: tc("status"),
      render: (r) =>
        r.outOfStock ? (
          <Badge tone="dead">{t("outOfStock")}</Badge>
        ) : r.lowStock ? (
          <Badge tone="warn">{t("lowStock")}</Badge>
        ) : (
          <Badge tone="ok">{t("healthy")}</Badge>
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
            📦 {t("adjust")}
          </Button>
          <Button size="sm" variant="ghost" onClick={() => setHistoryFor(r)} aria-label={tA11y("history")}>
            🕘
          </Button>
          <Button size="sm" variant="ghost" onClick={() => openEdit(r)} aria-label={tA11y("edit", { name: r.name })}>
            ✏️
          </Button>
          <Button size="sm" variant="danger" onClick={() => setDeleting(r)} aria-label={tA11y("delete", { name: r.name })}>
            🗑️
          </Button>
        </div>
      ),
    },
  ];

  const movementColumns: Column<Movement>[] = [
    {
      key: "when",
      header: tc("date"),
      render: (m) => new Date(m.createdAt).toLocaleString(),
    },
    {
      key: "type",
      header: t("movementType"),
      render: (m) => <Badge tone={MOVEMENT_TONE[m.type]}>{tMv(m.type)}</Badge>,
    },
    {
      key: "qty",
      header: tc("qty"),
      numeric: true,
      // Signed for the reader even though the stored quantity is always
      // positive — a column of bare numbers makes an addition and a removal
      // look alike.
      render: (m) => (
        <span className={m.increase ? "text-success" : "text-danger"}>
          {m.increase ? "+" : "−"}
          {m.qty}
        </span>
      ),
    },
    {
      key: "balance",
      header: t("balanceAfter"),
      numeric: true,
      // The column the ledger exists for: the figure the screen showed after
      // this row, so a wrong count can be traced to the row that made it wrong.
      // A dash means the movement predates the ledger and its balance was never
      // recorded — see V11.
      // `== null`, not `=== null`: the server omits null columns, so an absent
      // balance arrives as undefined.
      render: (m) =>
        m.balanceAfter == null ? (
          <span className="text-ink-400">—</span>
        ) : (
          <b className="font-[family-name:var(--font-num)]">{m.balanceAfter}</b>
        ),
    },
    { key: "by", header: t("movementBy"), render: (m) => m.createdBy ?? "—" },
    {
      key: "why",
      header: tc("reason"),
      render: (m) => (
        <>
          <div>{m.reason ?? "—"}</div>
          {m.refType && (
            <div className="text-[11px] text-ink-500">{m.refType}</div>
          )}
        </>
      ),
    },
  ];

  /**
   * What moved. Only the whole ledger needs it — a history modal is already
   * about one thing, and repeating its name down every row says nothing.
   */
  const subjectColumn: Column<Movement> = {
    key: "subject",
    header: t("subject"),
    render: (m) => (
      <>
          <span className="font-medium">{m.productName ?? m.stockItemName ?? "—"}</span>
          <div className="text-[11px] text-ink-500">
            {m.productId == null ? t("ingredient") : t("dish")}
          </div>
      </>
    ),
  };

  const ledgerColumns: Column<Movement>[] = [
    movementColumns[0],
    subjectColumn,
    ...movementColumns.slice(1),
  ];

  return (
    <ListPage>
      {list.isError && <Alert tone="error">{apiError(list.error)}</Alert>}

      <StatGrid>
        <StatTile tone={1} label={t("totalItems")} value={summary.data?.totalItems ?? "—"} />
        <StatTile tone={3} label={t("stockValue")} value={formatUsd(summary.data?.stockValue ?? 0)} />
        <StatTile tone={2} label={t("lowStock")} value={summary.data?.lowStockCount ?? "—"} />
        <StatTile tone={4} label={t("outOfStock")} value={summary.data?.outOfStockCount ?? "—"} />
      </StatGrid>

      <Tabs
        items={[
          { id: "levels", label: t("levels") },
          { id: "ledger", label: t("ledger") },
        ]}
        active={tab}
        onChange={(id) => setTab(id as "levels" | "ledger")}
      />

      {tab === "levels" ? (
      <>
        <Toolbar
          left={
            <>
              <Button variant="admin" onClick={openNew}>
                ➕ {t("addItem")}
              </Button>
              <Select
                className="w-auto"
                value={onlyLow ? "low" : "all"}
                onChange={(e) => setOnlyLow(e.target.value === "low")}
                aria-label={tA11y("filter")}
              >
                <option value="all">{tc("all")}</option>
                <option value="low">{t("lowStockOnly")}</option>
              </Select>
            </>
          }
          right={<SearchBar value={search} onChange={(v) => { setSearch(v); setPage(0); }} />}
        />

        <DataTable
          fill
          columns={columns}
          rows={rows}
          rowKey={(r) => r.id}
          loading={list.isLoading}
          emptyMessage={t("noItems")}
        />

        {/* The low-stock tab filters the rows already loaded rather than
            asking the server, so it is a single page whose count is what the
            filter left. Reporting the unfiltered total there would be a lie,
            and reporting zero — which it briefly did — a worse one. The row is
            always rendered so the table above keeps its height either way. */}
        <Pagination
          page={onlyLow ? 0 : (list.data?.page ?? 0)}
          totalPages={onlyLow ? 1 : (list.data?.totalPages ?? 0)}
          totalElements={onlyLow ? rows.length : (list.data?.totalElements ?? 0)}
          size={onlyLow ? Math.max(rows.length, 1) : (list.data?.size ?? size)}
          onPage={setPage}
          onSize={(n) => {
            setSize(n);
            setPage(0);
          }}
        />
      </>
      ) : (
      <>
        {ledger.isError && <Alert tone="error">{apiError(ledger.error)}</Alert>}

        <p className="mb-3.5 text-sm text-ink-500">{t("ledgerHelp")}</p>

        <DataTable
          fill
          columns={ledgerColumns}
          rows={ledger.data?.content ?? []}
          rowKey={(m) => m.id}
          loading={ledger.isLoading}
          emptyMessage={t("noMovements")}
        />

        <Pagination
          page={ledger.data?.page ?? 0}
          totalPages={ledger.data?.totalPages ?? 0}
          totalElements={ledger.data?.totalElements ?? 0}
          size={ledger.data?.size ?? ledgerSize}
          onPage={setLedgerPage}
          onSize={(n) => {
            setLedgerSize(n);
            setLedgerPage(0);
          }}
        />
      </>
      )}

      {/* ---- create / edit ---- */}
      <Modal
        open={editingId !== undefined}
        onClose={() => setEditingId(undefined)}
        title={t("details")}
        width="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setEditingId(undefined)}>
              {tc("close")}
            </Button>
            <Button variant="admin" onClick={submit} loading={save.isPending}>
              {tc("save")}
            </Button>
          </>
        }
      >
        {formError && <Alert tone="error">{formError}</Alert>}

        <FieldRow>
          <Field label={tc("name")} htmlFor="k-name" required>
            <Input id="k-name" value={draft.name} onChange={(e) => set("name", e.target.value)} placeholder={t("namePlaceholder")} />
          </Field>
          <Field label={tc("unit")} htmlFor="k-unit" required>
            <Input id="k-unit" value={draft.unit} onChange={(e) => set("unit", e.target.value)} placeholder={t("unitPlaceholder")} />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field
            label={tc("qty")}
            htmlFor="k-qty"
            hint={editingId !== null ? t("qtyDirectHint") : undefined}
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
          <Field label={t("minQty")} htmlFor="k-min">
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

        <Field label={t("unitCost")} htmlFor="k-cost">
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
        title={`${t("adjustTitle")} — ${adjusting?.name ?? ""}`}
        width="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setAdjusting(null)}>
              {tc("close")}
            </Button>
            <Button variant="admin" onClick={() => adjust.mutate()} loading={adjust.isPending}>
              {tc("save")}
            </Button>
          </>
        }
      >
        {adjustError && <Alert tone="error">{adjustError}</Alert>}

        <p className="mb-3 text-sm text-ink-500">
          {t("currently")}{" "}
          <b className="text-ink-900">
            {adjusting?.qty} {adjusting?.unit}
          </b>
        </p>

        <FieldRow>
          <Field label={t("movementType")} htmlFor="a-type" required>
            <Select
              id="a-type"
              value={adjustDraft.type}
              onChange={(e) =>
                setAdjustDraft({ ...adjustDraft, type: e.target.value as MovementType })
              }
            >
              <option value="IN">{tMv("IN")}</option>
              <option value="OUT">{tMv("OUT")}</option>
              <option value="DAMAGED">{tMv("DAMAGED")}</option>
            </Select>
          </Field>
          <Field
            label={tc("qty")}
            htmlFor="a-qty"
            required
            hint={t("qtyHint")}
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

        <Field label={tc("reason")} htmlFor="a-why">
          <Textarea
            id="a-why"
            value={adjustDraft.reason ?? ""}
            onChange={(e) => setAdjustDraft({ ...adjustDraft, reason: e.target.value })}
            placeholder={t("reasonPlaceholder")}
          />
        </Field>
      </Modal>

      {/* ---- movement history ---- */}
      <Modal
        open={historyFor !== null}
        onClose={() => setHistoryFor(null)}
        title={`${t("movements")} — ${historyFor?.name ?? ""}`}
        width="lg"
        footer={
          <Button variant="light" onClick={() => setHistoryFor(null)}>
            {tc("close")}
          </Button>
        }
      >
        <DataTable
          columns={movementColumns}
          height="46vh"
          rows={movements.data?.content ?? []}
          rowKey={(m) => m.id}
          loading={movements.isLoading}
          emptyMessage={t("noMovements")}
        />
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        message={tc("confirmDeleteMessage", { name: deleting?.name ?? "" })}
        detail={t("deleteNote")}
      />
    </ListPage>
  );
}
