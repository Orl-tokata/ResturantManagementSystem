"use client";

import { useState } from "react";
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
  Toolbar,
  toneForTableStatus,
  type Column,
} from "@/components/ui";
import { useList, useRemove, useSave } from "@/hooks/useCrud";
import { useQuery } from "@tanstack/react-query";
import { get } from "@/lib/api";
import { errorMessage } from "@/lib/errors";
import {
  TABLE_STATUS_LABEL,
  ZONE_LABEL,
  type DiningTable,
  type TableRequest,
  type TableStatus,
  type TableSummary,
  type TableZone,
} from "@/types/master";

const EMPTY: TableRequest = { name: "", seats: 4, zone: "INDOOR", status: "FREE" };
const SIZE = 50;

export default function TablesPage() {
  const [search, setSearch] = useState("");
  const [zone, setZone] = useState<TableZone | "">("");
  const [page, setPage] = useState(0);

  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  const [draft, setDraft] = useState<TableRequest>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<DiningTable | null>(null);
  const [listError, setListError] = useState<string | null>(null);

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
      setFormError("ឈ្មោះតុត្រូវការ · Table name is required");
      return;
    }
    if (!draft.seats || draft.seats < 1) {
      setFormError("ចំនួនអាសនៈត្រូវយ៉ាងតិច ១ · Seats must be at least 1");
      return;
    }
    try {
      await save.mutateAsync({ id: editingId ?? null, body: draft });
      setEditingId(undefined);
    } catch (e) {
      setFormError(errorMessage(e, "Could not save the table"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      setListError(errorMessage(e, "Could not delete the table"));
    } finally {
      setDeleting(null);
    }
  }

  const columns: Column<DiningTable>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * SIZE + i + 1 },
    { key: "name", header: "ឈ្មោះតុ · Table name", render: (r) => <span className="font-medium">{r.name}</span> },
    { key: "seats", header: "អាសនៈ · Seats", numeric: true, render: (r) => r.seats },
    { key: "zone", header: "ទីតាំង · Zone", hideOnMobile: true, render: (r) => ZONE_LABEL[r.zone] },
    {
      key: "status",
      header: "ស្ថានភាព · Status",
      render: (r) => <Badge tone={toneForTableStatus(r.status)}>{TABLE_STATUS_LABEL[r.status]}</Badge>,
    },
    {
      key: "actions",
      header: "សកម្មភាព",
      align: "right",
      render: (r) => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="ghost" onClick={() => openEdit(r)} aria-label={`Edit ${r.name}`}>
            ✏️
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => setDeleting(r)}
            aria-label={`Delete ${r.name}`}
          >
            🗑️
          </Button>
        </div>
      ),
    },
  ];

  return (
    <>
      {listError && <Alert tone="error">{listError}</Alert>}
      {list.isError && <Alert tone="error">{errorMessage(list.error)}</Alert>}

      <StatGrid>
        <StatTile tone={1} label="ទំនេរ · Free" value={summary.data?.free ?? "—"} />
        <StatTile tone={4} label="កំពុងប្រើ · Occupied" value={summary.data?.occupied ?? "—"} />
        <StatTile tone={2} label="កក់ទុក · Reserved" value={summary.data?.reserved ?? "—"} />
        <StatTile tone={3} label="សរុប · Total" value={summary.data?.total ?? "—"} />
      </StatGrid>

      <Toolbar
        left={
          <>
            <Button variant="admin" onClick={openNew}>
              ➕ បន្ថែមតុ · Add table
            </Button>
            <Select
              className="w-auto"
              value={zone}
              onChange={(e) => {
                setZone(e.target.value as TableZone | "");
                setPage(0);
              }}
            >
              <option value="">ទីតាំងទាំងអស់ · All zones</option>
              <option value="INDOOR">{ZONE_LABEL.INDOOR}</option>
              <option value="OUTDOOR">{ZONE_LABEL.OUTDOOR}</option>
              <option value="VIP">{ZONE_LABEL.VIP}</option>
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
            placeholder="ស្វែងរកតុ · Search table"
          />
        }
      />

      <DataTable
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage="គ្មានតុ · No tables found"
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

      <Modal
        open={editingId !== undefined}
        onClose={() => setEditingId(undefined)}
        title="ព័ត៌មានតុ · Table Details"
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
          <Field label="ឈ្មោះតុ · Table name" htmlFor="t-name" required>
            <Input
              id="t-name"
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              placeholder="Table 13"
            />
          </Field>
          <Field label="អាសនៈ · Seats" htmlFor="t-seats" required>
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
          <Field label="ទីតាំង · Zone" htmlFor="t-zone">
            <Select
              id="t-zone"
              value={draft.zone}
              onChange={(e) => setDraft({ ...draft, zone: e.target.value as TableZone })}
            >
              <option value="INDOOR">{ZONE_LABEL.INDOOR}</option>
              <option value="OUTDOOR">{ZONE_LABEL.OUTDOOR}</option>
              <option value="VIP">{ZONE_LABEL.VIP}</option>
            </Select>
          </Field>
          <Field label="ស្ថានភាព · Status" htmlFor="t-status">
            <Select
              id="t-status"
              value={draft.status}
              onChange={(e) => setDraft({ ...draft, status: e.target.value as TableStatus })}
            >
              <option value="FREE">{TABLE_STATUS_LABEL.FREE}</option>
              <option value="OCCUPIED">{TABLE_STATUS_LABEL.OCCUPIED}</option>
              <option value="RESERVED">{TABLE_STATUS_LABEL.RESERVED}</option>
            </Select>
          </Field>
        </FieldRow>
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        message={`តើអ្នកប្រាកដជាចង់លុប "${deleting?.name ?? ""}" មែនទេ?`}
        detail={
          deleting?.status === "OCCUPIED"
            ? "This table is occupied, so the server will refuse to delete it."
            : "This action cannot be undone."
        }
      />
    </>
  );
}
