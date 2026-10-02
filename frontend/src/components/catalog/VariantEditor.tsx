"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Alert, Button, Field, Input } from "@/components/ui";
import { del, get, post, put } from "@/lib/api";
import { formatUsd } from "@/lib/format";
import { useApiError } from "@/lib/use-api-error";
import type { Variant, VariantRequest } from "@/types/catalog-extras";

/**
 * A product's sizes, inside the product's own form.
 *
 * <p>SCREENS §4 rejects a separate variant manager outright: a second screen
 * means two places to look for one product's price. So this lives in the edit
 * modal, under the price it replaces.
 *
 * <p>It saves immediately rather than with the rest of the form. A variant
 * belongs to a product that already exists — there is nothing to hang it on
 * until the product has an id — and batching the two would mean inventing a
 * way to post them together for no gain.
 */
export function VariantEditor({ productId }: { productId: number }) {
  const t = useTranslations("variants");
  const tc = useTranslations("common");
  const apiError = useApiError();
  const qc = useQueryClient();

  const [name, setName] = useState("");
  const [price, setPrice] = useState("");
  const [error, setError] = useState<string | null>(null);

  const variants = useQuery({
    queryKey: ["products", productId, "variants"],
    queryFn: () => get<Variant[]>(`/products/${productId}/variants`),
  });

  function refresh() {
    void qc.invalidateQueries({ queryKey: ["products", productId, "variants"] });
    // The grid shows a flag for "has options", so the list it came from is
    // stale the moment the first size is added.
    void qc.invalidateQueries({ queryKey: ["products"] });
  }

  const add = useMutation({
    mutationFn: () =>
      post<Variant>(`/products/${productId}/variants`, {
        name: name.trim(),
        price: Number(price),
      } satisfies VariantRequest),
    onSuccess: () => {
      setName("");
      setPrice("");
      refresh();
    },
    onError: (e) => setError(apiError(e, "saveVariant")),
  });

  const rename = useMutation({
    mutationFn: (v: Variant) =>
      put<Variant>(`/products/${productId}/variants/${v.id}`, {
        name: v.name,
        price: v.price,
      } satisfies VariantRequest),
    onSuccess: refresh,
    onError: (e) => setError(apiError(e, "saveVariant")),
  });

  const remove = useMutation({
    mutationFn: (id: number) => del<void>(`/products/${productId}/variants/${id}`),
    onSuccess: refresh,
    onError: (e) => setError(apiError(e, "deleteVariant")),
  });

  const rows = variants.data ?? [];

  return (
    <section className="mt-4 rounded border border-ink-200 p-3">
      <h3 className="mb-1 text-sm font-semibold">{t("title")}</h3>
      <p className="mb-3 text-xs text-ink-500">{t("help")}</p>

      {error && <Alert tone="error">{error}</Alert>}

      {rows.length > 0 && (
        <ul className="mb-3 space-y-1.5">
          {rows.map((v) => (
            <li key={v.id} className="flex items-center gap-2">
              <Input
                className="flex-1"
                value={v.name}
                onChange={(e) =>
                  qc.setQueryData<Variant[]>(["products", productId, "variants"], (list) =>
                    (list ?? []).map((x) => (x.id === v.id ? { ...x, name: e.target.value } : x)),
                  )
                }
                onBlur={() => rename.mutate(v)}
                aria-label={t("name")}
              />
              <Input
                className="w-24"
                type="number"
                step="0.01"
                min="0"
                value={v.price}
                onChange={(e) =>
                  qc.setQueryData<Variant[]>(["products", productId, "variants"], (list) =>
                    (list ?? []).map((x) =>
                      x.id === v.id ? { ...x, price: Number(e.target.value) } : x,
                    ),
                  )
                }
                onBlur={() => rename.mutate(v)}
                aria-label={tc("price")}
              />
              <span className="w-16 text-right text-xs text-ink-500">{formatUsd(v.price)}</span>
              <Button
                size="sm"
                variant="danger"
                onClick={() => remove.mutate(v.id)}
                aria-label={t("removeSize", { name: v.name })}
              >
                🗑️
              </Button>
            </li>
          ))}
        </ul>
      )}

      <div className="flex items-end gap-2">
        <Field label={t("name")} htmlFor="v-name">
          <Input
            id="v-name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder={t("namePlaceholder")}
          />
        </Field>
        <Field label={tc("price")} htmlFor="v-price">
          <Input
            id="v-price"
            type="number"
            step="0.01"
            min="0"
            className="w-28"
            value={price}
            onChange={(e) => setPrice(e.target.value)}
          />
        </Field>
        <Button
          variant="light"
          className="mb-0.5"
          disabled={!name.trim() || !price}
          loading={add.isPending}
          onClick={() => {
            setError(null);
            add.mutate();
          }}
        >
          ➕ {tc("add")}
        </Button>
      </div>
    </section>
  );
}
