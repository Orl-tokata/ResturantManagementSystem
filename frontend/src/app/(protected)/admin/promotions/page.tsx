"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useQueryClient } from "@tanstack/react-query";
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
  Select,
  Toolbar,
  useToast,
} from "@/components/ui";
import { useAll, useList, useRemove, useSave } from "@/hooks/useCrud";
import { useApiError } from "@/lib/use-api-error";
import { formatUsd } from "@/lib/format";
import type { Category, Product } from "@/types/master";
import {
  PROMOTION_SCOPES,
  PROMOTION_TYPES,
  type Promotion,
  type PromotionRequest,
  type PromotionScope,
  type PromotionType,
} from "@/types/promotion";

/** Today at nine, to a fortnight's time — a window somebody would actually type. */
function defaultWindow() {
  const from = new Date();
  from.setHours(0, 0, 0, 0);
  const to = new Date(from);
  to.setDate(to.getDate() + 14);
  const iso = (d: Date) => d.toISOString().slice(0, 16);
  return { startsAt: iso(from), endsAt: iso(to) };
}

const EMPTY: PromotionRequest = {
  name: "",
  type: "PERCENT",
  value: 10,
  scope: "ORDER",
  active: true,
  ...defaultWindow(),
};

export default function PromotionsPage() {
  const t = useTranslations("promotions");
  const tc = useTranslations("common");
  const tType = useTranslations("enum.promotionType");
  const tScope = useTranslations("enum.promotionScope");
  const tA11y = useTranslations("a11y");
  const apiError = useApiError();
  const toast = useToast();
  const qc = useQueryClient();

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [editingId, setEditingId] = useState<number | null | undefined>(undefined);
  const [draft, setDraft] = useState<PromotionRequest>(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<Promotion | null>(null);

  const list = useList<Promotion>("promotions", { page, size });
  const save = useSave<Promotion, PromotionRequest>("promotions");
  const remove = useRemove("promotions");

  // Only needed while the form is open, and only for the scope that uses them.
  const products = useAll<Product>("products");
  const categories = useAll<Category>("categories");

  function set<K extends keyof PromotionRequest>(key: K, value: PromotionRequest[K]) {
    setDraft((d) => ({ ...d, [key]: value }));
  }

  function openNew() {
    setEditingId(null);
    setDraft(EMPTY);
    setFormError(null);
  }

  function openEdit(p: Promotion) {
    setEditingId(p.id);
    setDraft({
      name: p.name,
      type: p.type,
      value: p.value,
      scope: p.scope,
      scopeId: p.scopeId,
      minAmount: p.minAmount,
      startsAt: p.startsAt.slice(0, 16),
      endsAt: p.endsAt.slice(0, 16),
      timeFrom: p.timeFrom?.slice(0, 5),
      timeTo: p.timeTo?.slice(0, 5),
      active: p.active,
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
        body: {
          ...draft,
          // A whole-bill rule applies to everything, so it names nothing —
          // sending a stale id from a previous scope choice is a 400.
          scopeId: draft.scope === "ORDER" ? undefined : draft.scopeId,
          timeFrom: draft.timeFrom || undefined,
          timeTo: draft.timeTo || undefined,
          minAmount: draft.minAmount || undefined,
        },
      });
      setEditingId(undefined);
      void qc.invalidateQueries({ queryKey: ["promotions"] });
    } catch (e) {
      setFormError(apiError(e, "savePromotion"));
    }
  }

  const columns: Column<Promotion>[] = [
    {
      key: "name",
      header: tc("name"),
      render: (p) => (
        <>
          <span className="font-medium">{p.name}</span>
          {/* The question an owner actually has when looking at this list. */}
          {p.liveNow && (
            <span className="ml-2">
              <Badge tone="ok">{t("liveNow")}</Badge>
            </span>
          )}
          {!p.active && (
            <span className="ml-2">
              <Badge tone="neutral">{t("off")}</Badge>
            </span>
          )}
        </>
      ),
    },
    {
      key: "what",
      header: t("takesOff"),
      render: (p) =>
        p.type === "PERCENT" ? `${p.value}%` : formatUsd(p.value),
    },
    {
      key: "scope",
      header: t("appliesTo"),
      render: (p) => (
        <>
          {tScope(p.scope)}
          {p.scopeName && <span className="ml-1 text-ink-500">· {p.scopeName}</span>}
        </>
      ),
    },
    {
      key: "when",
      header: t("window"),
      hideOnMobile: true,
      render: (p) => (
        <>
          <div>
            {new Date(p.startsAt).toLocaleDateString()} –{" "}
            {new Date(p.endsAt).toLocaleDateString()}
          </div>
          {p.timeFrom && p.timeTo && (
            <div className="text-[11px] text-ink-500">
              {p.timeFrom.slice(0, 5)}–{p.timeTo.slice(0, 5)}
            </div>
          )}
        </>
      ),
    },
    {
      key: "min",
      header: t("minimum"),
      numeric: true,
      hideOnMobile: true,
      render: (p) => (p.minAmount ? formatUsd(p.minAmount) : "—"),
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (p) => (
        <div className="flex justify-end gap-1.5">
          <Button size="sm" variant="ghost" onClick={() => openEdit(p)} aria-label={tA11y("edit", { name: p.name })}>
            ✏️
          </Button>
          <Button size="sm" variant="danger" onClick={() => setDeleting(p)} aria-label={tA11y("delete", { name: p.name })}>
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
      />

      <p className="mb-3 text-sm text-ink-500">{t("help")}</p>

      <DataTable
        fill
        columns={columns}
        rows={list.data?.content ?? []}
        rowKey={(p) => p.id}
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

        <Field label={tc("name")} htmlFor="pr-name" required hint={t("nameHint")}>
          <Input
            id="pr-name"
            value={draft.name}
            onChange={(e) => set("name", e.target.value)}
            placeholder={t("namePlaceholder")}
          />
        </Field>

        <FieldRow>
          <Field label={t("takesOff")} htmlFor="pr-type" required>
            <Select
              id="pr-type"
              value={draft.type}
              onChange={(e) => set("type", e.target.value as PromotionType)}
            >
              {PROMOTION_TYPES.map((type) => (
                <option key={type} value={type}>
                  {tType(type)}
                </option>
              ))}
            </Select>
          </Field>
          <Field
            label={draft.type === "PERCENT" ? t("percent") : tc("amount")}
            htmlFor="pr-value"
            required
          >
            <Input
              id="pr-value"
              type="number"
              step="0.01"
              min="0.01"
              max={draft.type === "PERCENT" ? 100 : undefined}
              value={draft.value}
              onChange={(e) => set("value", Number(e.target.value))}
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label={t("appliesTo")} htmlFor="pr-scope" required>
            <Select
              id="pr-scope"
              value={draft.scope}
              onChange={(e) =>
                setDraft({ ...draft, scope: e.target.value as PromotionScope, scopeId: undefined })
              }
            >
              {PROMOTION_SCOPES.map((scope) => (
                <option key={scope} value={scope}>
                  {tScope(scope)}
                </option>
              ))}
            </Select>
          </Field>

          {draft.scope !== "ORDER" && (
            <Field label={t("which")} htmlFor="pr-scope-id" required>
              <Select
                id="pr-scope-id"
                value={draft.scopeId ?? ""}
                onChange={(e) => set("scopeId", Number(e.target.value) || undefined)}
              >
                <option value="">{tc("choose")}</option>
                {(draft.scope === "ITEM" ? products.data ?? [] : categories.data ?? []).map(
                  (row) => (
                    <option key={row.id} value={row.id}>
                      {row.name}
                    </option>
                  ),
                )}
              </Select>
            </Field>
          )}
        </FieldRow>

        <FieldRow>
          <Field label={t("from")} htmlFor="pr-start" required>
            <Input
              id="pr-start"
              type="datetime-local"
              value={draft.startsAt}
              onChange={(e) => set("startsAt", e.target.value)}
            />
          </Field>
          <Field label={t("to")} htmlFor="pr-end" required>
            <Input
              id="pr-end"
              type="datetime-local"
              value={draft.endsAt}
              onChange={(e) => set("endsAt", e.target.value)}
            />
          </Field>
        </FieldRow>

        {/* Happy hour. Left empty it runs all day; a window that crosses
            midnight is allowed and read as two halves. */}
        <FieldRow>
          <Field label={t("timeFrom")} htmlFor="pr-tfrom" hint={t("timeHint")}>
            <Input
              id="pr-tfrom"
              type="time"
              value={draft.timeFrom ?? ""}
              onChange={(e) => set("timeFrom", e.target.value)}
            />
          </Field>
          <Field label={t("timeTo")} htmlFor="pr-tto">
            <Input
              id="pr-tto"
              type="time"
              value={draft.timeTo ?? ""}
              onChange={(e) => set("timeTo", e.target.value)}
            />
          </Field>
        </FieldRow>

        <FieldRow>
          <Field label={t("minimum")} htmlFor="pr-min" hint={t("minimumHint")}>
            <Input
              id="pr-min"
              type="number"
              step="0.01"
              min="0"
              value={draft.minAmount ?? ""}
              onChange={(e) => set("minAmount", Number(e.target.value) || undefined)}
            />
          </Field>
          <Field label={tc("status")} htmlFor="pr-active">
            <Select
              id="pr-active"
              value={draft.active ? "on" : "off"}
              onChange={(e) => set("active", e.target.value === "on")}
            >
              <option value="on">{t("on")}</option>
              <option value="off">{t("off")}</option>
            </Select>
          </Field>
        </FieldRow>
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={async () => {
          if (!deleting) return;
          try {
            await remove.mutateAsync(deleting.id);
          } catch (e) {
            toast.error(apiError(e, "deletePromotion"));
          } finally {
            setDeleting(null);
          }
        }}
        message={tc("confirmDeleteMessage", { name: deleting?.name ?? "" })}
        detail={t("deleteNote")}
      />
    </ListPage>
  );
}
