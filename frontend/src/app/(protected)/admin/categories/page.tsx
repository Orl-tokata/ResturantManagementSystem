"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
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
import { useApiError } from "@/lib/use-api-error";
import {
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
  const t = useTranslations("categories");
  const tc = useTranslations("common");
  const tStatus = useTranslations("enum.recordStatus");
  const tA11y = useTranslations("a11y");
  const apiError = useApiError();

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
      setFormError(t("errName"));
      return;
    }
    try {
      await save.mutateAsync({ id: editingId ?? null, body: draft });
      setEditingId(undefined);
    } catch (e) {
      setFormError(apiError(e, "saveCategory"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      setListError(apiError(e, "deleteCategory"));
    } finally {
      setDeleting(null);
    }
  }

  const columns: Column<Category>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * SIZE + i + 1 },
    {
      key: "icon",
      header: t("icon"),
      width: "72px",
      align: "center",
      render: (r) => <span className="text-xl">{r.icon}</span>,
    },
    {
      key: "name",
      header: t("categoryName"),
      render: (r) => (
        <>
          <div className="font-medium">{r.name}</div>
          <div className="text-xs text-ink-500">{r.nameEn}</div>
        </>
      ),
    },
    {
      key: "count",
      header: t("dishCount"),
      numeric: true,
      hideOnMobile: true,
      render: (r) => r.productCount,
    },
    { key: "sort", header: t("sortOrder"), numeric: true, hideOnMobile: true, render: (r) => r.sortOrder },
    {
      key: "status",
      header: tc("status"),
      render: (r) => (
        <Badge tone={toneForRecordStatus(r.status)}>{tStatus(r.status)}</Badge>
      ),
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
    <>
      {listError && <Alert tone="error">{listError}</Alert>}
      {list.isError && <Alert tone="error">{apiError(list.error)}</Alert>}

      <Toolbar
        left={
          <Button variant="admin" onClick={openNew}>
            ➕ {t("addCategory")}
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
        emptyMessage={t("noCategories")}
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

        <Field label={t("categoryName")} htmlFor="c-name" required>
          <Input
            id="c-name"
            value={draft.name}
            onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            placeholder={t("namePlaceholder")}
          />
        </Field>

        <Field label={t("nameEn")} htmlFor="c-name-en">
          <Input
            id="c-name-en"
            value={draft.nameEn}
            onChange={(e) => setDraft({ ...draft, nameEn: e.target.value })}
            placeholder="Rice"
          />
        </Field>

        <FieldRow>
          <Field label={t("icon")} htmlFor="c-icon" hint="Emoji">
            <Input
              id="c-icon"
              value={draft.icon}
              onChange={(e) => setDraft({ ...draft, icon: e.target.value })}
              placeholder="🍚"
            />
          </Field>
          <Field label={t("sortOrder")} htmlFor="c-sort">
            <Input
              id="c-sort"
              type="number"
              value={draft.sortOrder ?? 0}
              onChange={(e) => setDraft({ ...draft, sortOrder: Number(e.target.value) })}
            />
          </Field>
        </FieldRow>

        <Field label={tc("status")} htmlFor="c-status">
          <Select
            id="c-status"
            value={draft.status}
            onChange={(e) => setDraft({ ...draft, status: e.target.value as RecordStatus })}
          >
            <option value="ACTIVE">{tStatus("ACTIVE")}</option>
            <option value="INACTIVE">{tStatus("INACTIVE")}</option>
          </Select>
        </Field>
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        message={tc("confirmDeleteMessage", { name: deleting?.name ?? "" })}
        detail={
          deleting && deleting.productCount > 0
            ? t("inUse", { count: deleting.productCount })
            : tc("cannotUndo")
        }
      />
    </>
  );
}
