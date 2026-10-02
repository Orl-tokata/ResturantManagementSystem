"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
import {
  Alert,
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
  Textarea,
  Toolbar,
  useToast,
} from "@/components/ui";
import { useList, useRemove, useSave } from "@/hooks/useCrud";
import { useApiError } from "@/lib/use-api-error";
import { formatUsd } from "@/lib/format";
import type { Customer, CustomerRequest } from "@/types/customer";

const EMPTY: CustomerRequest = { name: "", phone: "", email: "", note: "" };
const INITIAL_SIZE = 20;

export default function CustomersPage() {
  const t = useTranslations("customers");
  const tc = useTranslations("common");
  const tA11y = useTranslations("a11y");
  const apiError = useApiError();
  const toast = useToast();
  const qc = useQueryClient();

  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(INITIAL_SIZE);

  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  const [draft, setDraft] = useState<CustomerRequest>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<Customer | null>(null);

  const list = useList<Customer>("customers", { search, page, size });
  const save = useSave<Customer, CustomerRequest>("customers");
  const remove = useRemove("customers");

  function set<K extends keyof CustomerRequest>(key: K, value: CustomerRequest[K]) {
    setDraft((d) => ({ ...d, [key]: value }));
  }

  function openNew() {
    setEditingId(null);
    setDraft(EMPTY);
    setFormError(null);
  }

  function openEdit(c: Customer) {
    setEditingId(c.id);
    setDraft({
      name: c.name,
      phone: c.phone ?? "",
      email: c.email ?? "",
      birthDate: c.birthDate ?? "",
      note: c.note ?? "",
    });
    setFormError(null);
  }

  async function submit() {
    if (!draft.name.trim()) {
      setFormError(t("errName"));
      return;
    }
    try {
      await save.mutateAsync({
        id: editingId ?? null,
        // Empty strings are not values. A blank birthday sent as "" is a 400
        // from the date parser rather than "they did not say".
        body: {
          ...draft,
          phone: draft.phone?.trim() || undefined,
          email: draft.email?.trim() || undefined,
          birthDate: draft.birthDate || undefined,
          note: draft.note?.trim() || undefined,
        },
      });
      setEditingId(undefined);
      void qc.invalidateQueries({ queryKey: ["customers"] });
    } catch (e) {
      setFormError(apiError(e, "saveCustomer"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      toast.error(apiError(e, "deleteCustomer"));
    } finally {
      setDeleting(null);
    }
  }

  const columns: Column<Customer>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * size + i + 1 },
    {
      key: "name",
      header: tc("name"),
      render: (c) => (
        <Link href={`/admin/customers/${c.id}`} className="font-medium text-brand-600 hover:underline">
          {c.name}
        </Link>
      ),
    },
    { key: "code", header: t("code"), hideOnMobile: true, render: (c) => c.code },
    // The lookup key at the till, so it earns a column of its own.
    { key: "phone", header: t("phone"), render: (c) => c.phone ?? "—" },
    {
      key: "points",
      header: t("points"),
      numeric: true,
      render: (c) => <b className="font-[family-name:var(--font-num)]">{c.points}</b>,
    },
    {
      key: "spent",
      header: t("totalSpent"),
      numeric: true,
      hideOnMobile: true,
      render: (c) => formatUsd(c.totalSpent),
    },
    { key: "visits", header: t("visits"), numeric: true, hideOnMobile: true, render: (c) => c.visitCount },
    {
      key: "last",
      header: t("lastVisit"),
      hideOnMobile: true,
      // A dash, not "never": they may have been in before this screen existed.
      render: (c) => (c.lastVisit ? new Date(c.lastVisit).toLocaleDateString() : "—"),
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (c) => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="ghost" onClick={() => openEdit(c)} aria-label={tA11y("edit", { name: c.name })}>
            ✏️
          </Button>
          <Button size="sm" variant="danger" onClick={() => setDeleting(c)} aria-label={tA11y("delete", { name: c.name })}>
            🗑️
          </Button>
        </div>
      ),
    },
  ];

  return (
    <ListPage>
      {list.isError && <Alert tone="error">{apiError(list.error)}</Alert>}

      <Toolbar
        left={
          <Button variant="admin" onClick={openNew}>
            ➕ {t("add")}
          </Button>
        }
        right={
          <SearchBar
            value={search}
            onChange={(v) => {
              setSearch(v);
              setPage(0);
            }}
            placeholder={t("searchPlaceholder")}
          />
        }
      />

      <DataTable
        fill
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(c) => c.id}
        loading={list.isLoading}
        emptyMessage={t("none")}
      />

      <Pagination
        page={list.data?.page ?? 0}
        totalPages={list.data?.totalPages ?? 0}
        totalElements={list.data?.totalElements ?? 0}
        size={list.data?.size ?? size}
        onPage={setPage}
        onSize={(n) => {
          setSize(n);
          setPage(0);
        }}
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

        <Field label={tc("name")} htmlFor="c-name" required>
          <Input id="c-name" value={draft.name} onChange={(e) => set("name", e.target.value)} />
        </Field>

        <FieldRow>
          <Field label={t("phone")} htmlFor="c-phone" hint={t("phoneHint")}>
            <Input
              id="c-phone"
              inputMode="tel"
              value={draft.phone ?? ""}
              onChange={(e) => set("phone", e.target.value)}
              placeholder="012 345 678"
            />
          </Field>
          <Field label={tc("email")} htmlFor="c-email">
            <Input
              id="c-email"
              type="email"
              value={draft.email ?? ""}
              onChange={(e) => set("email", e.target.value)}
            />
          </Field>
        </FieldRow>

        <Field label={t("birthDate")} htmlFor="c-birth">
          <Input
            id="c-birth"
            type="date"
            value={draft.birthDate ?? ""}
            onChange={(e) => set("birthDate", e.target.value)}
          />
        </Field>

        <Field label={tc("note")} htmlFor="c-note">
          <Textarea id="c-note" value={draft.note ?? ""} onChange={(e) => set("note", e.target.value)} />
        </Field>
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
