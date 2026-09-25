"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
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
  Modal,
  Pagination,
  SearchBar,
  Select,
  StatGrid,
  StatTile,
  toneForTableStatus,
  Toolbar,
  useToast,
} from "@/components/ui";
import { useList, useRemove, useSave } from "@/hooks/useCrud";
import { useQuery } from "@tanstack/react-query";
import { get } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import {
  type DiningTable,
  type TableRequest,
  type TableStatus,
  type TableSummary,
  type TableZone,
} from "@/types/master";

const EMPTY: TableRequest = { name: "", seats: 4, zone: "INDOOR", status: "FREE" };
const SIZE = 50;

export default function TablesPage() {
  const t = useTranslations("tables");
  const tc = useTranslations("common");
  const tZone = useTranslations("enum.zone");
  const tStatus = useTranslations("enum.tableStatus");
  const tA11y = useTranslations("a11y");
  const apiError = useApiError();
  const toast = useToast();

  const [search, setSearch] = useState("");
  const [zone, setZone] = useState<TableZone | "">("");
  const [page, setPage] = useState(0);

  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  const [draft, setDraft] = useState<TableRequest>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<DiningTable | null>(null);

  const list = useList<DiningTable>("tables", {
    search,
    zone: zone === "" ? undefined : zone,
    page,
    size: SIZE,
  });
  const summary = useQuery({
    queryKey: ["tables", "summary"],
    queryFn: () => get<TableSummary>("/tables/summary"),
  });
  const save = useSave<DiningTable, TableRequest>("tables");
  const remove = useRemove("tables");

  function openNew() {
    setEditingId(null);
    setDraft(EMPTY);
    setFormError(null);
  }

  function openEdit(row: DiningTable) {
    setEditingId(row.id);
    setDraft({ name: row.name, seats: row.seats, zone: row.zone, status: row.status });
    setFormError(null);
  }

  async function submit() {
    if (!draft.name.trim()) {
      setFormError(t("tableName") + " — " + tc("required"));
      return;
    }
    if (!draft.seats || draft.seats < 1) {
      setFormError(t("seats") + " ≥ 1");
      return;
    }
    try {
      await save.mutateAsync({ id: editingId ?? null, body: draft });
      setEditingId(undefined);
    } catch (e) {
      setFormError(apiError(e, "saveTable"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      toast.error(apiError(e, "deleteTable"));
    } finally {
      setDeleting(null);
    }
  }

  const columns: Column<DiningTable>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * SIZE + i + 1 },
    { key: "name", header: t("tableName"), render: (r) => <span className="font-medium">{r.name}</span> },
    { key: "seats", header: t("seats"), numeric: true, render: (r) => r.seats },
    { key: "zone", header: t("zone"), hideOnMobile: true, render: (r) => tZone(r.zone) },
    {
      key: "status",
      header: tc("status"),
      render: (r) => <Badge tone={toneForTableStatus(r.status)}>{tStatus(r.status)}</Badge>,
    },
    {
      key: "actions",
      header: tc("actions"),
      align: "right",
      render: (r) => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="ghost" onClick={() => openEdit(r)} aria-label={tA11y("edit", { name: r.name })}>
            ✏️
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => setDeleting(r)}
            aria-label={tA11y("delete", { name: r.name })}
          >
            🗑️
          </Button>
        </div>
      ),
    },
  ];

  return (
    <ListPage>
      {list.isError && <Alert tone="error">{apiError(list.error)}</Alert>}

      <StatGrid>
        <StatTile tone={1} label={tStatus("FREE")} value={summary.data?.free ?? "—"} />
        <StatTile tone={4} label={tStatus("OCCUPIED")} value={summary.data?.occupied ?? "—"} />
        <StatTile tone={2} label={tStatus("RESERVED")} value={summary.data?.reserved ?? "—"} />
        <StatTile tone={3} label={tc("total")} value={summary.data?.total ?? "—"} />
      </StatGrid>

      <Toolbar
        left={
          <>
            <Button variant="admin" onClick={openNew}>
              ➕ {t("addTable")}
            </Button>
            <Select
              className="w-auto"
              value={zone}
              onChange={(e) => {
                setZone(e.target.value as TableZone | "");
                setPage(0);
              }}
            >
              <option value="">{t("allZones")}</option>
              <option value="INDOOR">{tZone("INDOOR")}</option>
              <option value="OUTDOOR">{tZone("OUTDOOR")}</option>
              <option value="VIP">{tZone("VIP")}</option>
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
            placeholder={t("searchTable")}
          />
        }
      />

      <DataTable
        fill
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage={t("noTables")}
      />

      {/* Always rendered, never conditional: on this layout the table
          claims whatever height the pager does not use, so a pager that
          appears when the data arrives would resize the table under the
          reader. */}
      <Pagination
        page={list.data?.page ?? 0}
        totalPages={list.data?.totalPages ?? 0}
        totalElements={list.data?.totalElements ?? 0}
        size={list.data?.size ?? SIZE}
        onPage={setPage}
      />

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
          <Field label={t("tableName")} htmlFor="t-name" required>
            <Input
              id="t-name"
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              placeholder="Table 13"
            />
          </Field>
          <Field label={t("seats")} htmlFor="t-seats" required>
            <Input
              id="t-seats"
              type="number"
              min="1"
              value={draft.seats}
              onChange={(e) => setDraft({ ...draft, seats: Number(e.target.value) })}
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label={t("zone")} htmlFor="t-zone">
            <Select
              id="t-zone"
              value={draft.zone}
              onChange={(e) => setDraft({ ...draft, zone: e.target.value as TableZone })}
            >
              <option value="INDOOR">{tZone("INDOOR")}</option>
              <option value="OUTDOOR">{tZone("OUTDOOR")}</option>
              <option value="VIP">{tZone("VIP")}</option>
            </Select>
          </Field>
          <Field label={tc("status")} htmlFor="t-status">
            <Select
              id="t-status"
              value={draft.status}
              onChange={(e) => setDraft({ ...draft, status: e.target.value as TableStatus })}
            >
              <option value="FREE">{tStatus("FREE")}</option>
              <option value="OCCUPIED">{tStatus("OCCUPIED")}</option>
              <option value="RESERVED">{tStatus("RESERVED")}</option>
            </Select>
          </Field>
        </FieldRow>
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        message={tc("confirmDeleteMessage", { name: deleting?.name ?? "" })}
        detail={
          deleting?.status === "OCCUPIED"
            ? t("occupiedWarning")
            : tc("cannotUndo")
        }
      />
    </ListPage>
  );
}
