"use client";

import { useState } from "react";
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
} from "@/components/ui";
import { get, put } from "@/lib/api";
import { errorMessage } from "@/lib/errors";

type Settings = Record<string, string>;

/** Toggle keys rendered as switches, in the order the prototype showed them. */
const TOGGLES: Array<[string, string]> = [
  ["option.autoPrint", "បោះពុម្ពវិក្កយបត្រស្វ័យប្រវត្តិ · Auto print receipt"],
  ["option.showKhr", "បង្ហាញតម្លៃជារៀល · Show KHR price"],
  ["option.lowStockAlert", "ជូនដំណឹងស្តុកជិតអស់ · Low stock alert"],
  ["option.requireTable", "តម្រូវឲ្យជ្រើសរើសតុ · Require table selection"],
  ["option.allowDiscount", "អនុញ្ញាតការបញ្ចុះតម្លៃ · Allow discount"],
];

export default function SettingsPage() {
  const qc = useQueryClient();
  const [draft, setDraft] = useState<Settings | null>(null);
  const [saved, setSaved] = useState<string | null>(null);
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
      setSaved("បានរក្សាទុក · Settings saved");
      setError(null);
      // Totals shown anywhere depend on the VAT and riel rate.
      void qc.invalidateQueries({ queryKey: ["order"] });
    },
    onError: (e) => {
      setError(errorMessage(e, "Could not save the settings"));
      setSaved(null);
    },
  });

  function set(key: string, value: string) {
    setDraft((d) => ({ ...(d ?? {}), [key]: value }));
    setSaved(null);
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
    return <p className="text-sm text-ink-500">កំពុងផ្ទុក… · Loading</p>;
  }

  if (settings.isError) {
    return <Alert tone="error">{errorMessage(settings.error)}</Alert>;
  }

  return (
    <>
      {error && <Alert tone="error">{error}</Alert>}
      {saved && <Alert tone="success">{saved}</Alert>}

      <div className="grid gap-4 lg:grid-cols-2 lg:items-start">
        {/* ---- restaurant identity ---- */}
        <Card title="ព័ត៌មានហាង · Restaurant information">
          <Field label="ឈ្មោះហាង · Name (Khmer)" htmlFor="s-name">
            <Input
              id="s-name"
              value={v("restaurant.name")}
              onChange={(e) => set("restaurant.name", e.target.value)}
            />
          </Field>
          <Field label="ឈ្មោះជាភាសាអង់គ្លេស · Name (English)" htmlFor="s-name-en">
            <Input
              id="s-name-en"
              value={v("restaurant.nameEn")}
              onChange={(e) => set("restaurant.nameEn", e.target.value)}
            />
          </Field>
          <FieldRow>
            <Field label="ទូរស័ព្ទ · Phone" htmlFor="s-phone">
              <Input
                id="s-phone"
                value={v("restaurant.phone")}
                onChange={(e) => set("restaurant.phone", e.target.value)}
              />
            </Field>
            <Field label="អ៊ីមែល · Email" htmlFor="s-email">
              <Input
                id="s-email"
                type="email"
                value={v("restaurant.email")}
                onChange={(e) => set("restaurant.email", e.target.value)}
              />
            </Field>
          </FieldRow>
          <Field label="អាសយដ្ឋាន · Address" htmlFor="s-addr">
            <Textarea
              id="s-addr"
              value={v("restaurant.address")}
              onChange={(e) => set("restaurant.address", e.target.value)}
            />
          </Field>
          <p className="mb-3 text-xs text-ink-500">These appear on every printed receipt.</p>
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
              រក្សាទុក · Save
            </Button>
          </div>
        </Card>

        <div className="space-y-4">
          {/* ---- sales settings ---- */}
          <Card title="ការកំណត់ការលក់ · Sales settings">
            <FieldRow>
              <Field label="រូបិយប័ណ្ណគោល · Base currency" htmlFor="s-cur">
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
                label="អត្រាប្តូរប្រាក់ · Exchange rate"
                htmlFor="s-rate"
                hint="Riel per 1 USD"
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
                label="អត្រាពន្ធ · VAT (%)"
                htmlFor="s-vat"
                hint="Applies to bills opened from now on"
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
              <Field label="បុព្វបទវិក្កយបត្រ · Invoice prefix" htmlFor="s-prefix">
                <Input
                  id="s-prefix"
                  value={v("sales.invoicePrefix")}
                  onChange={(e) => set("sales.invoicePrefix", e.target.value)}
                />
              </Field>
            </FieldRow>

            <Alert tone="info">
              ការផ្លាស់ប្តូរពន្ធមានប្រសិទ្ធភាពលើវិក្កយបត្រថ្មីតែប៉ុណ្ណោះ · A VAT change applies
              only to bills opened afterwards. Bills already open keep the rate they were
              opened with, so nothing is restated behind a cashier&apos;s back.
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
                រក្សាទុក · Save
              </Button>
            </div>
          </Card>

          {/* ---- toggles ---- */}
          <Card title="ជម្រើសប្រព័ន្ធ · System options">
            <div className="space-y-1">
              {TOGGLES.map(([key, label]) => (
                <div
                  key={key}
                  className="flex items-center justify-between border-b border-ink-200 py-2.5 text-sm last:border-0"
                >
                  <span>{label}</span>
                  <Checkbox
                    label=""
                    aria-label={label}
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
                រក្សាទុក · Save
              </Button>
            </div>
          </Card>
        </div>
      </div>
    </>
  );
}
