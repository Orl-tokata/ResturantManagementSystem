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
  Toolbar,
  toneForRecordStatus,
  type Column,
} from "@/components/ui";
import { useList, useRemove, useSave } from "@/hooks/useCrud";
import { errorMessage } from "@/lib/errors";
import {
  RECORD_STATUS_LABEL,
  type Category,
  type CategoryRequest,
  type RecordStatus,
} from "@/types/master";

const EMPTY: CategoryRequest = {
  name: "",
  nameEn: "",
  icon: "",
  sortOrder: 0,
  status: "ACTIVE",
};

const SIZE = 20;

export default function CategoriesPage() {
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);

  // undefined = modal closed, null = creating, number = editing that id
  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  const [draft, setDraft] = useState<CategoryRequest>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<Category | null>(null);
  const [listError, setListError] = useState<string | null>(null);

  const list = useList<Category>("categories", { search, page, size: SIZE });
  const save = useSave<Category, CategoryRequest>("categories");
  const remove = useRemove("categories");

  function openNew() {
    setEditingId(null);
    setDraft(EMPTY);
    setFormError(null);
  }

  function openEdit(row: Category) {
    setEditingId(row.id);
    setDraft({
      name: row.name,
      nameEn: row.nameEn ?? "",
      icon: row.icon ?? "",
      sortOrder: row.sortOrder,
      status: row.status,
    });
    setFormError(null);
  }

  async function submit() {
    if (!draft.name.trim()) {
      setFormError("ឈ្មោះប្រភេទត្រូវការ · Category name is required");
      return;
    }
    try {
      await save.mutateAsync({ id: editingId ?? null, body: draft });
      setEditingId(undefined);
    } catch (e) {
      setFormError(errorMessage(e, "Could not save the category"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      setListError(errorMessage(e, "Could not delete the category"));
    } finally {
      setDeleting(null);
    }
  }

  const columns: Column<Category>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * SIZE + i + 1 },
    {
      key: "icon",
      header: "រូបតំណាង",
      width: "72px",
      align: "center",
      render: (r) => <span className="text-xl">{r.icon}</span>,
    },
    {
      key: "name",
      header: "ឈ្មោះប្រភេទ · Category name",
      render: (r) => (
        <>
          <div className="font-medium">{r.name}</div>
          <div className="text-xs text-ink-500">{r.nameEn}</div>
        </>
      ),
    },
    {
      key: "count",
      header: "ចំនួនម្ហូប",
      numeric: true,
      hideOnMobile: true,
      render: (r) => r.productCount,
    },
    { key: "sort", header: "លំដាប់", numeric: true, hideOnMobile: true, render: (r) => r.sortOrder },
    {
      key: "status",
      header: "ស្ថានភាព",
      render: (r) => (
        <Badge tone={toneForRecordStatus(r.status)}>{RECORD_STATUS_LABEL[r.status]}</Badge>
      ),
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

      <Toolbar
        left={
          <Button variant="admin" onClick={openNew}>
            ➕ បន្ថែមប្រភេទ · Add category
          </Button>
        }
        right={
          <SearchBar
            value={search}
            onChange={(v) => {
              setSearch(v);
              setPage(0);
            }}
          />
        }
      />

      <DataTable
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage="គ្មានប្រភេទ · No categories found"
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
        title="ព័ត៌មានប្រភេទ · Category Details"
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

        <Field label="ឈ្មោះប្រភេទ · Name (Khmer)" htmlFor="c-name" required>
          <Input
            id="c-name"
            value={draft.name}
            onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            placeholder="បាយ"
          />
        </Field>

        <Field label="ឈ្មោះជាភាសាអង់គ្លេស · Name (English)" htmlFor="c-name-en">
          <Input
            id="c-name-en"
            value={draft.nameEn}
            onChange={(e) => setDraft({ ...draft, nameEn: e.target.value })}
            placeholder="Rice"
          />
        </Field>

        <FieldRow>
          <Field label="រូបតំណាង · Icon" htmlFor="c-icon" hint="Emoji">
            <Input
              id="c-icon"
              value={draft.icon}
              onChange={(e) => setDraft({ ...draft, icon: e.target.value })}
              placeholder="🍚"
            />
          </Field>
          <Field label="លំដាប់ · Sort order" htmlFor="c-sort">
            <Input
              id="c-sort"
              type="number"
              value={draft.sortOrder ?? 0}
              onChange={(e) => setDraft({ ...draft, sortOrder: Number(e.target.value) })}
            />
          </Field>
        </FieldRow>

        <Field label="ស្ថានភាព · Status" htmlFor="c-status">
          <Select
            id="c-status"
            value={draft.status}
            onChange={(e) => setDraft({ ...draft, status: e.target.value as RecordStatus })}
          >
            <option value="ACTIVE">{RECORD_STATUS_LABEL.ACTIVE}</option>
            <option value="INACTIVE">{RECORD_STATUS_LABEL.INACTIVE}</option>
          </Select>
        </Field>
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        message={`តើអ្នកប្រាកដជាចង់លុប "${deleting?.name ?? ""}" មែនទេ?`}
        detail={
          deleting && deleting.productCount > 0
            ? `This category still has ${deleting.productCount} product(s), so the server will refuse.`
            : "This action cannot be undone."
        }
      />
    </>
  );
}
