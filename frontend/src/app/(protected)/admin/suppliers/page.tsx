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
  Textarea,
  Toolbar,
  toneForRecordStatus,
  type Column,
} from "@/components/ui";
import { useList, useRemove, useSave } from "@/hooks/useCrud";
import { useApiError } from "@/lib/use-api-error";
import { formatUsd } from "@/lib/format";
import type { RecordStatus } from "@/types/master";
import {
  SUPPLY_TYPES,
  type Supplier,
  type SupplierRequest,
} from "@/types/supply";

const EMPTY: SupplierRequest = { supplierCode: "", company: "", status: "ACTIVE" };
const SIZE = 20;

export default function SuppliersPage() {
  const t = useTranslations("suppliers");
  const tc = useTranslations("common");
  const tStatus = useTranslations("enum.recordStatus");
  const tType = useTranslations("enum.supplyType");
  const tA11y = useTranslations("a11y");
  const apiError = useApiError();

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
      setFormError(t("errRequired"));
      return;
    }
    try {
      await save.mutateAsync({ id: editingId ?? null, body: draft });
      setEditingId(undefined);
    } catch (e) {
      setFormError(apiError(e, "saveSupplier"));
    }
  }

  async function confirmDelete() {
    if (!deleting) return;
    try {
      await remove.mutateAsync(deleting.id);
    } catch (e) {
      setListError(apiError(e, "deleteSupplier"));
    } finally {
      setDeleting(null);
    }
  }

  const columns: Column<Supplier>[] = [
    { key: "n", header: "#", width: "56px", render: (_r, i) => page * SIZE + i + 1 },
    { key: "code", header: tc("code"), render: (r) => r.supplierCode },
    {
      key: "company",
      header: t("company"),
      render: (r) => (
        <>
          <div className="font-medium">{r.company}</div>
          {r.email && <div className="text-xs text-ink-500">{r.email}</div>}
        </>
      ),
    },
    { key: "contact", header: t("contactPerson"), hideOnMobile: true, render: (r) => r.contactPerson ?? "—" },
    { key: "phone", header: tc("phone"), hideOnMobile: true, render: (r) => r.phone ?? "—" },
    {
      key: "type",
      header: t("supplyType"),
      hideOnMobile: true,
      render: (r) => (r.supplyType ? tType(r.supplyType) : "—"),
    },
    {
      key: "balance",
      header: t("payable"),
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
      header: tc("status"),
      render: (r) => (
        <Badge tone={toneForRecordStatus(r.status)}>{tStatus(r.status)}</Badge>
      ),
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (r) => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="ghost" onClick={() => openEdit(r)} aria-label={tA11y("edit", { name: r.company })}>
            ✏️
          </Button>
          <Button
            size="sm"
            variant="danger"
            onClick={() => setDeleting(r)}
            aria-label={tA11y("delete", { name: r.company })}
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
            ➕ {t("addSupplier")}
          </Button>
        }
        right={<SearchBar value={search} onChange={(v) => { setSearch(v); setPage(0); }} />}
      />

      <DataTable
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(r) => r.id}
        loading={list.isLoading}
        emptyMessage={t("noSuppliers")}
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
          <Field label={tc("code")} htmlFor="v-code" required>
            <Input
              id="v-code"
              value={draft.supplierCode}
              onChange={(e) => set("supplierCode", e.target.value)}
              placeholder="SUP-006"
            />
          </Field>
          <Field label={t("supplyType")} htmlFor="v-type">
            <Select
              id="v-type"
              value={draft.supplyType ?? ""}
              onChange={(e) => set("supplyType", e.target.value || undefined)}
            >
              <option value="">—</option>
              {SUPPLY_TYPES.map((ty) => (
                <option key={ty} value={ty}>
                  {tType(ty)}
                </option>
              ))}
            </Select>
          </Field>
        </FieldRow>

        <Field label={t("company")} htmlFor="v-company" required>
          <Input
            id="v-company"
            value={draft.company}
            onChange={(e) => set("company", e.target.value)}
          />
        </Field>

        <FieldRow>
          <Field label={t("contactPerson")} htmlFor="v-contact">
            <Input
              id="v-contact"
              value={draft.contactPerson ?? ""}
              onChange={(e) => set("contactPerson", e.target.value || undefined)}
            />
          </Field>
          <Field label={tc("phone")} htmlFor="v-phone">
            <Input
              id="v-phone"
              value={draft.phone ?? ""}
              onChange={(e) => set("phone", e.target.value || undefined)}
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label={tc("email")} htmlFor="v-email">
            <Input
              id="v-email"
              type="email"
              value={draft.email ?? ""}
              onChange={(e) => set("email", e.target.value || undefined)}
            />
          </Field>
          <Field label={tc("status")} htmlFor="v-status">
            <Select
              id="v-status"
              value={draft.status}
              onChange={(e) => set("status", e.target.value as RecordStatus)}
            >
              <option value="ACTIVE">{tStatus("ACTIVE")}</option>
              <option value="INACTIVE">{tStatus("INACTIVE")}</option>
            </Select>
          </Field>
        </FieldRow>

        <Field label={tc("address")} htmlFor="v-addr">
          <Textarea
            id="v-addr"
            value={draft.address ?? ""}
            onChange={(e) => set("address", e.target.value || undefined)}
          />
        </Field>

        {editingId !== null && (
          <p className="text-xs text-ink-500">
            {t("balanceReadOnly")}
          </p>
        )}
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={confirmDelete}
        message={tc("confirmDeleteMessage", { name: deleting?.company ?? "" })}
        detail={
          deleting && deleting.balance > 0
            ? t("outstanding", { amount: formatUsd(deleting.balance) })
            : tc("cannotUndo")
        }
      />
    </>
  );
}
