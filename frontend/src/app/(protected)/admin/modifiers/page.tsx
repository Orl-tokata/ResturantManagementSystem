"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import {
  Alert,
  Badge,
  Button,
  Card,
  ConfirmDialog,
  Field,
  FieldRow,
  Input,
  ListPage,
  Modal,
  Toolbar,
  useToast,
} from "@/components/ui";
import { del, get, post, put } from "@/lib/api";
import { formatUsd } from "@/lib/format";
import { useApiError } from "@/lib/use-api-error";
import type { ModifierGroup, ModifierGroupRequest } from "@/types/catalog-extras";

type DraftModifier = { name: string; priceDelta: string };

const EMPTY = {
  name: "",
  minSelect: 0,
  maxSelect: 1,
  modifiers: [{ name: "", priceDelta: "0" }] as DraftModifier[],
};

/**
 * The questions the menu can ask.
 *
 * <p>A screen of their own because the groups are shared: "sugar level" is one
 * question however many drinks ask it, so editing it inside any one product
 * would be editing it for all of them from a place that does not say so.
 * Sizes are the opposite case and live in the product form — SCREENS §4.
 */
export default function ModifierGroupsPage() {
  const t = useTranslations("modifiers");
  const tc = useTranslations("common");
  const tA11y = useTranslations("a11y");
  const apiError = useApiError();
  const toast = useToast();
  const qc = useQueryClient();

  const [editing, setEditing] = useState<ModifierGroup | null | undefined>(undefined);
  const [draft, setDraft] = useState(EMPTY);
  const [formError, setFormError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<ModifierGroup | null>(null);

  const groups = useQuery({
    queryKey: ["modifier-groups"],
    queryFn: () => get<ModifierGroup[]>("/modifier-groups"),
  });

  function refresh() {
    void qc.invalidateQueries({ queryKey: ["modifier-groups"] });
    // Attaching changes whether a product has options, which the POS reads.
    void qc.invalidateQueries({ queryKey: ["products"] });
  }

  const save = useMutation({
    mutationFn: () => {
      const body: ModifierGroupRequest = {
        name: draft.name.trim(),
        minSelect: Number(draft.minSelect),
        maxSelect: Number(draft.maxSelect),
        modifiers: draft.modifiers
          .filter((m) => m.name.trim())
          .map((m) => ({ name: m.name.trim(), priceDelta: Number(m.priceDelta || 0) })),
      };
      return editing
        ? put<ModifierGroup>(`/modifier-groups/${editing.id}`, body)
        : post<ModifierGroup>("/modifier-groups", body);
    },
    onSuccess: () => {
      setEditing(undefined);
      refresh();
    },
    onError: (e) => setFormError(apiError(e, "saveModifierGroup")),
  });

  const remove = useMutation({
    mutationFn: (id: number) => del<void>(`/modifier-groups/${id}`),
    onSuccess: refresh,
    onError: (e) => toast.error(apiError(e, "deleteModifierGroup")),
  });

  function openNew() {
    setEditing(null);
    setDraft(EMPTY);
    setFormError(null);
  }

  function openEdit(group: ModifierGroup) {
    setEditing(group);
    setDraft({
      name: group.name,
      minSelect: group.minSelect,
      maxSelect: group.maxSelect,
      modifiers: group.modifiers.map((m) => ({
        name: m.name,
        priceDelta: String(m.priceDelta),
      })),
    });
    setFormError(null);
  }

  function setModifier(index: number, patch: Partial<DraftModifier>) {
    setDraft((d) => ({
      ...d,
      modifiers: d.modifiers.map((m, i) => (i === index ? { ...m, ...patch } : m)),
    }));
  }

  const usable = draft.name.trim() && draft.modifiers.some((m) => m.name.trim());

  return (
    <ListPage>
      {groups.isError && <Alert tone="error">{apiError(groups.error)}</Alert>}

      <Toolbar
        left={
          <Button variant="admin" onClick={openNew}>
            ➕ {t("add")}
          </Button>
        }
      />

      <p className="mb-3 text-sm text-ink-500">{t("help")}</p>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {(groups.data ?? []).map((group) => (
          <Card
            key={group.id}
            title={group.name}
            action={
              <div className="flex gap-1.5">
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => openEdit(group)}
                  aria-label={tA11y("edit", { name: group.name })}
                >
                  ✏️
                </Button>
                <Button
                  size="sm"
                  variant="danger"
                  onClick={() => setDeleting(group)}
                  aria-label={tA11y("delete", { name: group.name })}
                >
                  🗑️
                </Button>
              </div>
            }
          >
            <div className="mb-2">
              <Badge tone={group.required ? "warn" : "neutral"}>
                {group.required ? t("mustChoose") : t("optional")}
              </Badge>
              <span className="ml-2 text-xs text-ink-500">
                {t("range", { min: group.minSelect, max: group.maxSelect })}
              </span>
            </div>

            <ul className="space-y-1 text-sm">
              {group.modifiers.map((m) => (
                <li key={m.id} className="flex justify-between gap-2">
                  <span>{m.name}</span>
                  <span className="font-[family-name:var(--font-num)] text-ink-500">
                    {m.priceDelta === 0
                      ? t("free")
                      : `${m.priceDelta > 0 ? "+" : "−"}${formatUsd(Math.abs(m.priceDelta))}`}
                  </span>
                </li>
              ))}
            </ul>
          </Card>
        ))}
      </div>

      {!groups.isLoading && (groups.data ?? []).length === 0 && (
        <p className="py-10 text-center text-sm text-ink-500">{t("none")}</p>
      )}

      <Modal
        open={editing !== undefined}
        onClose={() => setEditing(undefined)}
        title={t("details")}
        width="sm"
        footer={
          <>
            <Button variant="light" onClick={() => setEditing(undefined)}>
              {tc("close")}
            </Button>
            <Button
              variant="admin"
              onClick={() => {
                setFormError(null);
                save.mutate();
              }}
              loading={save.isPending}
              disabled={!usable}
            >
              {tc("save")}
            </Button>
          </>
        }
      >
        {formError && <Alert tone="error">{formError}</Alert>}

        <Field label={tc("name")} htmlFor="g-name" required hint={t("nameHint")}>
          <Input
            id="g-name"
            value={draft.name}
            onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            placeholder={t("namePlaceholder")}
          />
        </Field>

        {/* The pair is the shape of the question: 0–1 is "anything else?",
            1–1 is "choose one", 0–n is "tick what you want". */}
        <FieldRow>
          <Field label={t("minSelect")} htmlFor="g-min" hint={t("minHint")}>
            <Input
              id="g-min"
              type="number"
              min="0"
              value={draft.minSelect}
              onChange={(e) => setDraft({ ...draft, minSelect: Number(e.target.value) })}
            />
          </Field>
          <Field label={t("maxSelect")} htmlFor="g-max">
            <Input
              id="g-max"
              type="number"
              min="1"
              value={draft.maxSelect}
              onChange={(e) => setDraft({ ...draft, maxSelect: Number(e.target.value) })}
            />
          </Field>
        </FieldRow>

        <h3 className="mb-1.5 mt-3 text-sm font-semibold">{t("answers")}</h3>
        <ul className="space-y-1.5">
          {draft.modifiers.map((m, i) => (
            <li key={i} className="flex items-center gap-2">
              <Input
                className="flex-1"
                value={m.name}
                onChange={(e) => setModifier(i, { name: e.target.value })}
                placeholder={t("answerPlaceholder")}
                aria-label={t("answerName", { n: i + 1 })}
              />
              <Input
                className="w-24"
                type="number"
                step="0.01"
                value={m.priceDelta}
                onChange={(e) => setModifier(i, { priceDelta: e.target.value })}
                aria-label={t("answerPrice", { n: i + 1 })}
              />
              <Button
                size="sm"
                variant="ghost"
                onClick={() =>
                  setDraft((d) => ({
                    ...d,
                    modifiers: d.modifiers.filter((_, x) => x !== i),
                  }))
                }
                aria-label={t("removeAnswer", { n: i + 1 })}
              >
                ✕
              </Button>
            </li>
          ))}
        </ul>

        <Button
          variant="light"
          size="sm"
          className="mt-2"
          onClick={() =>
            setDraft((d) => ({ ...d, modifiers: [...d.modifiers, { name: "", priceDelta: "0" }] }))
          }
        >
          ➕ {t("addAnswer")}
        </Button>

        <p className="mt-3 text-xs text-ink-500">{t("deltaHint")}</p>
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        busy={remove.isPending}
        onClose={() => setDeleting(null)}
        onConfirm={() => {
          if (deleting) remove.mutate(deleting.id);
          setDeleting(null);
        }}
        message={tc("confirmDeleteMessage", { name: deleting?.name ?? "" })}
        detail={t("deleteNote")}
      />
    </ListPage>
  );
}
