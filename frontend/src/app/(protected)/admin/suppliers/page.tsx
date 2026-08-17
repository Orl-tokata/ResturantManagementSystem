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
  Textarea,
  Toolbar,
  toneForRecordStatus,
  type Column,
} from "@/components/ui";
import { useList, useRemove, useSave } from "@/hooks/useCrud";
import { errorMessage } from "@/lib/errors";
import { formatUsd } from "@/lib/format";
import { RECORD_STATUS_LABEL, type RecordStatus } from "@/types/master";
import {
  SUPPLY_TYPES,
  SUPPLY_TYPE_LABEL,
  type Supplier,
  type SupplierRequest,
} from "@/types/supply";

const EMPTY: SupplierRequest = { supplierCode: "", company: "", status: "ACTIVE" };
const SIZE = 20;

export default function SuppliersPage() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);

  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  const [draft, setDraft] = useState<SupplierRequest>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<Supplier | null>(null);
  const [listError, setListError] = useState<string | null>(null);

  const list = useList<Supplier>("suppliers", { search, page, size: SIZE });
  const save = useSave<Supplier, SupplierRequest>("suppliers");
  const remove = useRemove("suppliers");

  function set<K extends keyof SupplierRequest>(key: K, value: SupplierRequest[K]) {
    setDraft((d) => ({ ...d, [key]: value }));
  }

  function openNew() {
    setEditingId(null);
    setDraft(EMPTY);
    setFormError(null);
  }

  function openEdit(r: Supplier) {
    setEditingId(r.id);
    setDraft({
      supplierCode: r.supplierCode,
      company: r.company,
      contactPerson: r.contactPerson ?? undefined,
      phone: r.phone ?? undefined,
      email: r.email ?? undefined,
      supplyType: r.supplyType ?? undefined,
      address: r.address ?? undefined,
      status: r.status,
    });
    setFormError(null);
  }

  async function submit() {
    if (!draft.supplierCode.trim() || !draft.company.trim()) {
      setFormError("លេខកូដ និងឈ្មោះក្រុមហ៊ុនត្រូវការ · Code and company are required");
      return;
    }
    try {
      await save.mutateAsync({ id: editingId ?? null, body: draft });
      setEditingId(undefined);
    } catch (e) {
      setFormError(errorMessage(e, "Could not save the supplier"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      setListError(errorMessage(e, "Could not delete the supplier"));
    } finally {
      setDeleting(null);
    }
  }

  const columns: Column<Supplier>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * SIZE + i + 1 },
    { key: "code", header: "លេខកូដ", render: (r) => r.supplierCode },
    {
      key: "company",
      header: "ឈ្មោះក្រុមហ៊ុន · Company",
      render: (r) => (
        <>
          <div className="font-medium">{r.company}</div>
          {r.email && <div className="text-xs text-ink-500">{r.email}</div>}
        </>
      ),
    },
    { key: "contact", header: "អ្នកទំនាក់ទំនង", hideOnMobile: true, render: (r) => r.contactPerson ?? "—" },
    { key: "phone", header: "ទូរស័ព្ទ", hideOnMobile: true, render: (r) => r.phone ?? "—" },
    {
      key: "type",
      header: "ប្រភេទទំនិញ",
      hideOnMobile: true,
      render: (r) => (r.supplyType ? SUPPLY_TYPE_LABEL[r.supplyType] ?? r.supplyType : "—"),
    },
    {
      key: "balance",
      header: "ជំពាក់ · Payable",
      numeric: true,
      render: (r) =>
        r.balance > 0 ? (
          <span className="font-semibold text-danger">{formatUsd(r.balance)}</span>
        ) : (
          formatUsd(0)
        ),
    },
    {
      key: "status",
      header: "ស្ថានភាព",
      render: (r) => (
        <Badge tone={toneForRecordStatus(r.status)}>{RECORD_STATUS_LABEL[r.status]}</Badge>
      ),
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (r) => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="ghost" onClick={() => openEdit(r)} aria-label={`Edit ${r.company}`}>
            ✏️
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => setDeleting(r)}
            aria-label={`Delete ${r.company}`}
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

      <Toolbar
        left={
          <Button variant="admin" onClick={openNew}>
            ➕ បន្ថែមអ្នកផ្គត់ផ្គង់ · Add supplier
          </Button>
        }
        right={<SearchBar value={search} onChange={(v) => { setSearch(v); setPage(0); }} />}
      />

      <DataTable
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage="គ្មានអ្នកផ្គត់ផ្គង់ · No suppliers found"
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
        title="ព័ត៌មានអ្នកផ្គត់ផ្គង់ · Supplier Details"
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
          <Field label="លេខកូដ · Code" htmlFor="v-code" required>
            <Input
              id="v-code"
              value={draft.supplierCode}
              onChange={(e) => set("supplierCode", e.target.value)}
              placeholder="SUP-006"
            />
          </Field>
          <Field label="ប្រភេទទំនិញ · Supply type" htmlFor="v-type">
            <Select
              id="v-type"
              value={draft.supplyType ?? ""}
              onChange={(e) => set("supplyType", e.target.value || undefined)}
            >
              <option value="">—</option>
              {SUPPLY_TYPES.map((t) => (
                <option key={t} value={t}>
                  {SUPPLY_TYPE_LABEL[t]}
                </option>
              ))}
            </Select>
          </Field>
        </FieldRow>

        <Field label="ឈ្មោះក្រុមហ៊ុន · Company" htmlFor="v-company" required>
          <Input
            id="v-company"
            value={draft.company}
            onChange={(e) => set("company", e.target.value)}
          />
        </Field>

        <FieldRow>
          <Field label="អ្នកទំនាក់ទំនង · Contact person" htmlFor="v-contact">
            <Input
              id="v-contact"
              value={draft.contactPerson ?? ""}
              onChange={(e) => set("contactPerson", e.target.value || undefined)}
            />
          </Field>
          <Field label="ទូរស័ព្ទ · Phone" htmlFor="v-phone">
            <Input
              id="v-phone"
              value={draft.phone ?? ""}
              onChange={(e) => set("phone", e.target.value || undefined)}
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label="អ៊ីមែល · Email" htmlFor="v-email">
            <Input
              id="v-email"
              type="email"
              value={draft.email ?? ""}
              onChange={(e) => set("email", e.target.value || undefined)}
            />
          </Field>
          <Field label="ស្ថានភាព · Status" htmlFor="v-status">
            <Select
              id="v-status"
              value={draft.status}
              onChange={(e) => set("status", e.target.value as RecordStatus)}
            >
              <option value="ACTIVE">{RECORD_STATUS_LABEL.ACTIVE}</option>
              <option value="INACTIVE">{RECORD_STATUS_LABEL.INACTIVE}</option>
            </Select>
          </Field>
        </FieldRow>

        <Field label="អាសយដ្ឋាន · Address" htmlFor="v-addr">
          <Textarea
            id="v-addr"
            value={draft.address ?? ""}
            onChange={(e) => set("address", e.target.value || undefined)}
          />
        </Field>

        {editingId !== null && (
          <p className="text-xs text-ink-500">
            ជំពាក់ · Payable is not editable here — it moves only when goods are received.
          </p>
        )}
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        message={`តើអ្នកប្រាកដជាចង់លុប "${deleting?.company ?? ""}" មែនទេ?`}
        detail={
          deleting && deleting.balance > 0
            ? `${formatUsd(deleting.balance)} is still outstanding, so the server will refuse.`
            : "This action cannot be undone."
        }
      />
    </>
  );
}
