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
  type BadgeTone,
  type Column,
} from "@/components/ui";
import { useList, useRemove, useSave } from "@/hooks/useCrud";
import { errorMessage } from "@/lib/errors";
import { formatUsd } from "@/lib/format";
import type { Role } from "@/types/auth";
import {
  GENDER_LABEL,
  ROLE_LABEL,
  SHIFT_LABEL,
  STAFF_STATUS_LABEL,
  type Gender,
  type Shift,
  type Staff,
  type StaffRequest,
  type StaffStatus,
} from "@/types/master";

const EMPTY: StaffRequest = {
  staffCode: "",
  staffName: "",
  role: "CASHIER",
  status: "ACTIVE",
};

const SIZE = 20;

const STATUS_TONE: Record<StaffStatus, BadgeTone> = {
  ACTIVE: "ok",
  ON_LEAVE: "warn",
  RESIGNED: "dead",
};

export default function StaffPage() {
  const [search, setSearch] = useState("");
  const [role, setRole] = useState<Role | "">("");
  const [page, setPage] = useState(0);

  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  const [draft, setDraft] = useState<StaffRequest>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<Staff | null>(null);
  const [listError, setListError] = useState<string | null>(null);

  const list = useList<Staff>("staff", {
    search,
    role: role === "" ? undefined : role,
    page,
    size: SIZE,
  });
  const save = useSave<Staff, StaffRequest>("staff");
  const remove = useRemove("staff");

  function openNew() {
    setEditingId(null);
    setDraft(EMPTY);
    setFormError(null);
  }

  function openEdit(row: Staff) {
    setEditingId(row.id);
    setDraft({
      staffCode: row.staffCode,
      staffName: row.staffName,
      gender: row.gender ?? undefined,
      dateOfBirth: row.dateOfBirth ?? undefined,
      phone: row.phone ?? undefined,
      email: row.email ?? undefined,
      role: row.role,
      shift: row.shift ?? undefined,
      salary: row.salary ?? undefined,
      hireDate: row.hireDate ?? undefined,
      address: row.address ?? undefined,
      status: row.status,
    });
    setFormError(null);
  }

  async function submit() {
    if (!draft.staffCode.trim()) {
      setFormError("លេខកូដបុគ្គលិកត្រូវការ · Staff code is required");
      return;
    }
    if (!draft.staffName.trim()) {
      setFormError("ឈ្មោះបុគ្គលិកត្រូវការ · Staff name is required");
      return;
    }
    try {
      await save.mutateAsync({ id: editingId ?? null, body: draft });
      setEditingId(undefined);
    } catch (e) {
      setFormError(errorMessage(e, "Could not save the staff record"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      setListError(errorMessage(e, "Could not delete the staff record"));
    } finally {
      setDeleting(null);
    }
  }

  function set<K extends keyof StaffRequest>(key: K, value: StaffRequest[K]) {
    setDraft((d) => ({ ...d, [key]: value }));
  }

  const columns: Column<Staff>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * SIZE + i + 1 },
    { key: "code", header: "លេខកូដ", render: (r) => r.staffCode },
    {
      key: "name",
      header: "ឈ្មោះ · Name",
      render: (r) => (
        <>
          <div className="font-medium">{r.staffName}</div>
          {r.email && <div className="text-xs text-ink-500">{r.email}</div>}
        </>
      ),
    },
    {
      key: "gender",
      header: "ភេទ",
      hideOnMobile: true,
      render: (r) => (r.gender ? GENDER_LABEL[r.gender] : "—"),
    },
    { key: "role", header: "តួនាទី · Role", render: (r) => ROLE_LABEL[r.role] },
    { key: "phone", header: "ទូរស័ព្ទ", hideOnMobile: true, render: (r) => r.phone ?? "—" },
    {
      key: "shift",
      header: "វេន · Shift",
      hideOnMobile: true,
      render: (r) => (r.shift ? SHIFT_LABEL[r.shift] : "—"),
    },
    {
      key: "salary",
      header: "ប្រាក់ខែ",
      numeric: true,
      hideOnMobile: true,
      render: (r) => (r.salary != null ? formatUsd(r.salary) : "—"),
    },
    {
      key: "status",
      header: "ស្ថានភាព",
      render: (r) => <Badge tone={STATUS_TONE[r.status]}>{STAFF_STATUS_LABEL[r.status]}</Badge>,
    },
    {
      key: "actions",
      header: "សកម្មភាព",
      align: "right",
      render: (r) => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="ghost" onClick={() => openEdit(r)} aria-label={`Edit ${r.staffName}`}>
            ✏️
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => setDeleting(r)}
            aria-label={`Delete ${r.staffName}`}
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
          <>
            <Button variant="admin" onClick={openNew}>
              ➕ បន្ថែមបុគ្គលិក · Add staff
            </Button>
            <Select
              className="w-auto"
              value={role}
              onChange={(e) => {
                setRole(e.target.value as Role | "");
                setPage(0);
              }}
            >
              <option value="">តួនាទីទាំងអស់ · All roles</option>
              {(Object.keys(ROLE_LABEL) as Role[]).map((r) => (
                <option key={r} value={r}>
                  {ROLE_LABEL[r]}
                </option>
              ))}
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
            placeholder="ស្វែងរកបុគ្គលិក · Search"
          />
        }
      />

      <DataTable
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage="គ្មានបុគ្គលិក · No staff found"
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
        title="បន្ថែម/កែបុគ្គលិក · Staff Details"
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
          <Field label="លេខកូដ · Staff code" htmlFor="s-code" required>
            <Input
              id="s-code"
              value={draft.staffCode}
              onChange={(e) => set("staffCode", e.target.value)}
              placeholder="EMP-040"
            />
          </Field>
          <Field label="ឈ្មោះ · Staff name" htmlFor="s-name" required>
            <Input
              id="s-name"
              value={draft.staffName}
              onChange={(e) => set("staffName", e.target.value)}
              placeholder="Sok Dara"
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label="ភេទ · Gender" htmlFor="s-gender">
            <Select
              id="s-gender"
              value={draft.gender ?? ""}
              onChange={(e) => set("gender", (e.target.value || undefined) as Gender | undefined)}
            >
              <option value="">—</option>
              <option value="MALE">{GENDER_LABEL.MALE}</option>
              <option value="FEMALE">{GENDER_LABEL.FEMALE}</option>
            </Select>
          </Field>
          <Field label="ថ្ងៃខែឆ្នាំកំណើត · Date of birth" htmlFor="s-dob">
            <Input
              id="s-dob"
              type="date"
              value={draft.dateOfBirth ?? ""}
              onChange={(e) => set("dateOfBirth", e.target.value || undefined)}
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label="ទូរស័ព្ទ · Phone" htmlFor="s-phone">
            <Input
              id="s-phone"
              value={draft.phone ?? ""}
              onChange={(e) => set("phone", e.target.value || undefined)}
              placeholder="012 345 678"
            />
          </Field>
          <Field label="អ៊ីមែល · Email" htmlFor="s-email">
            <Input
              id="s-email"
              type="email"
              value={draft.email ?? ""}
              onChange={(e) => set("email", e.target.value || undefined)}
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label="តួនាទី · Role" htmlFor="s-role" required>
            <Select
              id="s-role"
              value={draft.role}
              onChange={(e) => set("role", e.target.value as Role)}
            >
              {(Object.keys(ROLE_LABEL) as Role[]).map((r) => (
                <option key={r} value={r}>
                  {ROLE_LABEL[r]}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="វេន · Shift" htmlFor="s-shift">
            <Select
              id="s-shift"
              value={draft.shift ?? ""}
              onChange={(e) => set("shift", (e.target.value || undefined) as Shift | undefined)}
            >
              <option value="">—</option>
              <option value="MORNING">{SHIFT_LABEL.MORNING}</option>
              <option value="EVENING">{SHIFT_LABEL.EVENING}</option>
              <option value="FULL_TIME">{SHIFT_LABEL.FULL_TIME}</option>
            </Select>
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label="ប្រាក់ខែ · Salary ($)" htmlFor="s-salary">
            <Input
              id="s-salary"
              type="number"
              min="0"
              step="10"
              value={draft.salary ?? ""}
              onChange={(e) =>
                set("salary", e.target.value === "" ? undefined : Number(e.target.value))
              }
            />
          </Field>
          <Field label="ថ្ងៃចូលធ្វើការ · Hire date" htmlFor="s-hire">
            <Input
              id="s-hire"
              type="date"
              value={draft.hireDate ?? ""}
              onChange={(e) => set("hireDate", e.target.value || undefined)}
            />
          </Field>
        </FieldRow>

        <Field label="ស្ថានភាព · Status" htmlFor="s-status">
          <Select
            id="s-status"
            value={draft.status}
            onChange={(e) => set("status", e.target.value as StaffStatus)}
          >
            <option value="ACTIVE">{STAFF_STATUS_LABEL.ACTIVE}</option>
            <option value="ON_LEAVE">{STAFF_STATUS_LABEL.ON_LEAVE}</option>
            <option value="RESIGNED">{STAFF_STATUS_LABEL.RESIGNED}</option>
          </Select>
        </Field>

        <Field label="អាសយដ្ឋាន · Address" htmlFor="s-addr">
          <Textarea
            id="s-addr"
            value={draft.address ?? ""}
            onChange={(e) => set("address", e.target.value || undefined)}
          />
        </Field>
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        message={`តើអ្នកប្រាកដជាចង់លុប "${deleting?.staffName ?? ""}" មែនទេ?`}
      />
    </>
  );
}
