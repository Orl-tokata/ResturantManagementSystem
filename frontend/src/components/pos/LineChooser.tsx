"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Alert, Button, Modal } from "@/components/ui";
import { get } from "@/lib/api";
import { formatUsd } from "@/lib/format";
import type { ModifierGroup, ProductOptions, Variant } from "@/types/catalog-extras";
import type { Product } from "@/types/master";

/**
 * What size, and anything to add.
 *
 * <p>Only opens for the dishes that ask something — the grid tells the till
 * which those are with one boolean, so an ordinary plate of rice is still a
 * single tap. Asking every time would make the common case slower to serve a
 * case that is rare.
 *
 * <p>The price shown here is worked out locally so the cashier can see what
 * they are about to charge. The server recalculates all of it from the ids;
 * this figure never leaves the screen.
 */
export function LineChooser({
  product,
  onCancel,
  onAdd,
}: {
  product: Product;
  onCancel: () => void;
  onAdd: (choice: {
    variant?: Variant;
    modifiers: { id: number; name: string; priceDelta: number }[];
    unitPrice: number;
  }) => void;
}) {
  const t = useTranslations("pos");
  const tc = useTranslations("common");

  const options = useQuery({
    queryKey: ["product", product.id, "options"],
    queryFn: async (): Promise<ProductOptions> => {
      const [variants, groups] = await Promise.all([
        get<Variant[]>(`/products/${product.id}/variants`),
        get<ModifierGroup[]>(`/products/${product.id}/modifier-groups`),
      ]);
      return { variants, groups };
    },
  });

  // Memoised because `chosen` below depends on them: a fresh [] on every
  // render would make that useMemo recompute every time and defeat itself.
  const variants = useMemo(() => options.data?.variants ?? [], [options.data]);
  const groups = useMemo(() => options.data?.groups ?? [], [options.data]);

  // The first size is pre-selected: a dish with sizes has no price without
  // one, and leaving it blank only invites the cashier to tap twice.
  const [variantId, setVariantId] = useState<number | null>(null);
  const chosenVariant = variants.find((v) => v.id === (variantId ?? variants[0]?.id));

  const [picked, setPicked] = useState<Record<number, number[]>>({});

  function toggle(group: ModifierGroup, modifierId: number) {
    setPicked((current) => {
      const inGroup = current[group.id] ?? [];
      if (inGroup.includes(modifierId)) {
        return { ...current, [group.id]: inGroup.filter((id) => id !== modifierId) };
      }
      // At the limit, the newest choice replaces the oldest rather than being
      // refused: "choose one" should feel like a radio button, not an error.
      const next =
        inGroup.length >= group.maxSelect
          ? [...inGroup.slice(1 - group.maxSelect), modifierId]
          : [...inGroup, modifierId];
      return { ...current, [group.id]: next };
    });
  }

  const chosen = useMemo(() => {
    const byId = new Map(groups.flatMap((g) => g.modifiers).map((m) => [m.id, m]));
    return Object.values(picked)
      .flat()
      .map((id) => byId.get(id))
      .filter((m): m is NonNullable<typeof m> => m != null)
      .map((m) => ({ id: m.id, name: m.name, priceDelta: m.priceDelta }));
  }, [picked, groups]);

  const unitPrice =
    (chosenVariant?.price ?? product.price) +
    chosen.reduce((sum, m) => sum + m.priceDelta, 0);

  /** Every question that has to be answered, has been. */
  const unanswered = groups.filter(
    (g) => (picked[g.id]?.length ?? 0) < g.minSelect,
  );

  return (
    <Modal
      open
      onClose={onCancel}
      title={product.name}
      width="sm"
      footer={
        <>
          <Button variant="light" onClick={onCancel}>
            {tc("cancel")}
          </Button>
          <Button
            variant="accent"
            disabled={options.isLoading || unanswered.length > 0}
            onClick={() =>
              onAdd({ variant: chosenVariant, modifiers: chosen, unitPrice })
            }
          >
            {t("addFor", { price: formatUsd(unitPrice) })}
          </Button>
        </>
      }
    >
      {options.isLoading && <p className="text-sm text-ink-500">{tc("loading")}</p>}

      {variants.length > 0 && (
        <fieldset className="mb-4">
          <legend className="mb-1.5 text-sm font-semibold">{t("size")}</legend>
          <div className="flex flex-wrap gap-2">
            {variants.map((v) => {
              const active = v.id === chosenVariant?.id;
              return (
                <button
                  key={v.id}
                  type="button"
                  onClick={() => setVariantId(v.id)}
                  aria-pressed={active}
                  className={`rounded border px-3 py-2 text-sm ${
                    active
                      ? "border-orange-500 bg-orange-500 text-white"
                      : "border-ink-300 hover:bg-ink-100"
                  }`}
                >
                  {v.name}
                  <span className="ml-2 font-[family-name:var(--font-num)]">
                    {formatUsd(v.price)}
                  </span>
                </button>
              );
            })}
          </div>
        </fieldset>
      )}

      {groups.map((group) => (
        <fieldset key={group.id} className="mb-4">
          <legend className="mb-1.5 text-sm font-semibold">
            {group.name}
            {group.required && <span className="ml-0.5 text-danger">*</span>}
            <span className="ml-2 text-xs font-normal text-ink-500">
              {t("chooseRange", { min: group.minSelect, max: group.maxSelect })}
            </span>
          </legend>

          <div className="flex flex-wrap gap-2">
            {group.modifiers.map((m) => {
              const active = (picked[group.id] ?? []).includes(m.id);
              return (
                <button
                  key={m.id}
                  type="button"
                  onClick={() => toggle(group, m.id)}
                  aria-pressed={active}
                  className={`rounded border px-3 py-2 text-sm ${
                    active
                      ? "border-teal-600 bg-teal-600 text-white"
                      : "border-ink-300 hover:bg-ink-100"
                  }`}
                >
                  {m.name}
                  {m.priceDelta !== 0 && (
                    <span className="ml-2 font-[family-name:var(--font-num)]">
                      {m.priceDelta > 0 ? "+" : "−"}
                      {formatUsd(Math.abs(m.priceDelta))}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </fieldset>
      ))}

      {unanswered.length > 0 && (
        <Alert tone="warn">
          {t("stillToAnswer", { names: unanswered.map((g) => g.name).join(", ") })}
        </Alert>
      )}
    </Modal>
  );
}
