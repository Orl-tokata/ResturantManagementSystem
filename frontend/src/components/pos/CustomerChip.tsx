"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Alert, Button, Field, Input, Modal } from "@/components/ui";
import { get, post } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import type { Customer } from "@/types/customer";

/**
 * Naming the customer on a bill, from the till.
 *
 * <p>A search by phone number rather than a page, because this happens with
 * somebody standing at the counter: one field, one tap, done. Phone is the
 * key here and not email — it is what a cashier can ask for out loud, and what
 * the person actually knows by heart.
 *
 * <p>The lookup can return more than one person, because a number is shared
 * between a couple as often as not. The screen shows both and lets the cashier
 * choose; picking the first would quietly put the meal on the wrong account.
 */
export function CustomerChip({
  customerId,
  customerName,
  onPick,
  disabled,
}: {
  customerId?: number;
  customerName?: string;
  onPick: (customerId: number | null) => Promise<void>;
  disabled?: boolean;
}) {
  const t = useTranslations("customers");
  const tc = useTranslations("common");
  const apiError = useApiError();

  const [open, setOpen] = useState(false);
  const [phone, setPhone] = useState("");
  const [found, setFound] = useState<Customer[] | null>(null);
  const [newName, setNewName] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function reset() {
    setPhone("");
    setFound(null);
    setNewName("");
    setError(null);
  }

  async function lookup() {
    setError(null);
    setBusy(true);
    try {
      setFound(await get<Customer[]>("/customers/lookup", { phone: phone.trim() }));
    } catch (e) {
      setError(apiError(e, "lookupCustomer"));
    } finally {
      setBusy(false);
    }
  }

  async function pick(id: number | null) {
    setBusy(true);
    try {
      await onPick(id);
      setOpen(false);
      reset();
    } catch (e) {
      setError(apiError(e, "setCustomer"));
    } finally {
      setBusy(false);
    }
  }

  /** Registering at the counter, because sending them away to a form is how a loyalty scheme dies. */
  async function registerAndPick() {
    setBusy(true);
    try {
      const created = await post<Customer>("/customers", {
        name: newName.trim(),
        phone: phone.trim(),
      });
      await onPick(created.id);
      setOpen(false);
      reset();
    } catch (e) {
      setError(apiError(e, "saveCustomer"));
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <button
        type="button"
        disabled={disabled}
        onClick={() => setOpen(true)}
        className="flex w-full items-center justify-between gap-2 border-t border-ink-300 px-3 py-1.5 text-left text-xs hover:bg-black/5 disabled:opacity-50"
      >
        <span className="text-ink-500">{t("customer")}</span>
        <span className="truncate font-semibold">
          {customerName ?? <span className="font-normal text-ink-500">{t("walkIn")}</span>}
        </span>
      </button>

      <Modal
        open={open}
        onClose={() => {
          setOpen(false);
          reset();
        }}
        title={t("customer")}
        width="sm"
        footer={
          <>
            {customerId != null && (
              <Button variant="light" onClick={() => pick(null)} loading={busy}>
                {t("clear")}
              </Button>
            )}
            <Button
              variant="light"
              onClick={() => {
                setOpen(false);
                reset();
              }}
            >
              {tc("close")}
            </Button>
          </>
        }
      >
        {error && <Alert tone="error">{error}</Alert>}

        <form
          onSubmit={(e) => {
            e.preventDefault();
            void lookup();
          }}
        >
          <Field label={t("phone")} htmlFor="pos-phone" hint={t("lookupHint")}>
            <div className="flex gap-2">
              <Input
                id="pos-phone"
                inputMode="tel"
                autoFocus
                value={phone}
                onChange={(e) => {
                  setPhone(e.target.value);
                  setFound(null);
                }}
                placeholder="012 345 678"
              />
              <Button type="submit" variant="primary" loading={busy} disabled={!phone.trim()}>
                {tc("search")}
              </Button>
            </div>
          </Field>
        </form>

        {found != null && found.length > 0 && (
          <ul className="mt-3 space-y-1.5">
            {found.map((c) => (
              <li key={c.id}>
                <button
                  type="button"
                  onClick={() => pick(c.id)}
                  disabled={busy}
                  className="flex w-full items-center justify-between gap-3 rounded border border-ink-200 px-3 py-2 text-left text-sm hover:bg-ink-100"
                >
                  <span>
                    <b className="block">{c.name}</b>
                    <span className="text-xs text-ink-500">{c.code}</span>
                  </span>
                  <span className="font-[family-name:var(--font-num)] text-sm">
                    {c.points} {t("pointsShort")}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}

        {found != null && found.length === 0 && (
          <div className="mt-3 rounded border border-dashed border-ink-300 p-3">
            <p className="mb-2 text-sm text-ink-500">{t("noneWithThatPhone")}</p>
            <Field label={tc("name")} htmlFor="pos-new-name">
              <div className="flex gap-2">
                <Input
                  id="pos-new-name"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder={t("namePlaceholder")}
                />
                <Button
                  variant="primary"
                  onClick={() => void registerAndPick()}
                  loading={busy}
                  disabled={!newName.trim()}
                >
                  {t("register")}
                </Button>
              </div>
            </Field>
          </div>
        )}
      </Modal>
    </>
  );
}
