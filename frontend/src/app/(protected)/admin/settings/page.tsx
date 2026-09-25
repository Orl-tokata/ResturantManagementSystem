"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Field,
  FieldRow,
  Input,
  Select,
  Textarea,
  useToast,
} from "@/components/ui";
import { get, put } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";

type Settings = Record<string, string>;

/** Toggle keys rendered as switches, in the order the prototype showed them. */
const TOGGLES: Array<[string, string]> = [
  ["option.autoPrint", "autoPrint"],
  ["option.showKhr", "showKhr"],
  ["option.lowStockAlert", "lowStockAlert"],
  ["option.requireTable", "requireTable"],
  ["option.allowDiscount", "allowDiscount"],
];

export default function SettingsPage() {
  const t = useTranslations("settings");
  const tc = useTranslations("common");
  const apiError = useApiError();
  const toast = useToast();

  const qc = useQueryClient();
  const [draft, setDraft] = useState<Settings | null>(null);
  const [error, setError] = useState<string | null>(null);

  const settings = useQuery({
    queryKey: ["settings"],
    queryFn: () => get<Settings>("/settings"),
  });

  // Adjusting state during render rather than syncing from an effect.
  const [loadedFor, setLoadedFor] = useState<Settings | null>(null);
  if (settings.data && loadedFor !== settings.data) {
    setLoadedFor(settings.data);
    setDraft({ ...settings.data });
  }

  const save = useMutation({
    mutationFn: (patch: Settings) => put<Settings>("/settings", patch),
    onSuccess: (fresh) => {
      qc.setQueryData(["settings"], fresh);
      toast.success(t("saved"));
      setError(null);
      // Totals shown anywhere depend on the VAT and riel rate.
      void qc.invalidateQueries({ queryKey: ["order"] });
    },
    onError: (e) => {
      setError(apiError(e, "saveSettings"));
    },
  });

  function set(key: string, value: string) {
    setDraft((d) => ({ ...(d ?? {}), [key]: value }));
  }

  function v(key: string) {
    return draft?.[key] ?? "";
  }

  function saveGroup(keys: string[]) {
    if (!draft) return;
    const patch: Settings = {};
    keys.forEach((k) => {
      if (draft[k] !== undefined) patch[k] = draft[k];
    });
    save.mutate(patch);
  }

  if (settings.isLoading) {
    return <p className="text-sm text-ink-500">{tc("loading")}</p>;
  }

  if (settings.isError) {
    return <Alert tone="error">{apiError(settings.error)}</Alert>;
  }

  return (
    <>
      {error && <Alert tone="error">{error}</Alert>}

      <div className="grid gap-4 lg:grid-cols-2 lg:items-start">
        {/* ---- restaurant identity ---- */}
        <Card title={t("restaurantInfo")}>
          <Field label={t("nameKm")} htmlFor="s-name">
            <Input
              id="s-name"
              value={v("restaurant.name")}
              onChange={(e) => set("restaurant.name", e.target.value)}
            />
          </Field>
          <Field label={t("nameEn")} htmlFor="s-name-en">
            <Input
              id="s-name-en"
              value={v("restaurant.nameEn")}
              onChange={(e) => set("restaurant.nameEn", e.target.value)}
            />
          </Field>
          <FieldRow>
            <Field label={tc("phone")} htmlFor="s-phone">
              <Input
                id="s-phone"
                value={v("restaurant.phone")}
                onChange={(e) => set("restaurant.phone", e.target.value)}
              />
            </Field>
            <Field label={tc("email")} htmlFor="s-email">
              <Input
                id="s-email"
                type="email"
                value={v("restaurant.email")}
                onChange={(e) => set("restaurant.email", e.target.value)}
              />
            </Field>
          </FieldRow>
          <Field label={tc("address")} htmlFor="s-addr">
            <Textarea
              id="s-addr"
              value={v("restaurant.address")}
              onChange={(e) => set("restaurant.address", e.target.value)}
            />
          </Field>
          <p className="mb-3 text-xs text-ink-500">{t("receiptNote")}</p>
          <div className="flex justify-end">
            <Button
              variant="admin"
              loading={save.isPending}
              onClick={() =>
                saveGroup([
                  "restaurant.name",
                  "restaurant.nameEn",
                  "restaurant.phone",
                  "restaurant.email",
                  "restaurant.address",
                ])
              }
            >
              {tc("save")}
            </Button>
          </div>
        </Card>

        <div className="space-y-4">
          {/* ---- sales settings ---- */}
          <Card title={t("salesSettings")}>
            <FieldRow>
              <Field label={t("baseCurrency")} htmlFor="s-cur">
                <Select
                  id="s-cur"
                  value={v("currency.base") || "USD"}
                  onChange={(e) => set("currency.base", e.target.value)}
                >
                  <option value="USD">USD ($)</option>
                  <option value="KHR">KHR (៛)</option>
                </Select>
              </Field>
              <Field
                label={t("exchangeRate")}
                htmlFor="s-rate"
                hint={t("exchangeHint")}
              >
                <Input
                  id="s-rate"
                  type="number"
                  min="1"
                  value={v("currency.khrRate")}
                  onChange={(e) => set("currency.khrRate", e.target.value)}
                />
              </Field>
            </FieldRow>

            <FieldRow>
              <Field
                label={t("vatRate")}
                htmlFor="s-vat"
                hint={t("vatHint")}
              >
                <Input
                  id="s-vat"
                  type="number"
                  min="0"
                  max="100"
                  step="0.5"
                  value={v("sales.vatRate")}
                  onChange={(e) => set("sales.vatRate", e.target.value)}
                />
              </Field>
              <Field label={t("invoicePrefix")} htmlFor="s-prefix">
                <Input
                  id="s-prefix"
                  value={v("sales.invoicePrefix")}
                  onChange={(e) => set("sales.invoicePrefix", e.target.value)}
                />
              </Field>
            </FieldRow>

            <Alert tone="info">
              {t("vatWarning")}
            </Alert>

            <div className="flex justify-end">
              <Button
                variant="admin"
                loading={save.isPending}
                onClick={() =>
                  saveGroup([
                    "currency.base",
                    "currency.khrRate",
                    "sales.vatRate",
                    "sales.invoicePrefix",
                  ])
                }
              >
                {tc("save")}
              </Button>
            </div>
          </Card>

          {/* ---- toggles ---- */}
          <Card title={t("systemOptions")}>
            <div className="space-y-1">
              {TOGGLES.map(([key, label]) => (
                <div
                  key={key}
                  className="flex items-center justify-between border-b border-ink-200 py-2.5 text-sm last:border-0"
                >
                  <span>{t(label)}</span>
                  <Checkbox
                    label=""
                    aria-label={t(label)}
                    checked={v(key) === "true"}
                    onChange={(e) => set(key, e.target.checked ? "true" : "false")}
                  />
                </div>
              ))}
            </div>
            <div className="mt-3 flex justify-end">
              <Button
                variant="admin"
                loading={save.isPending}
                onClick={() => saveGroup(TOGGLES.map(([k]) => k))}
              >
                {tc("save")}
              </Button>
            </div>
          </Card>
        </div>
      </div>
    </>
  );
}
