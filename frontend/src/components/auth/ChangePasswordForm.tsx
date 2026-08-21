"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Alert, Button, Field, FieldRow, Input } from "@/components/ui";
import { post } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";

/** Same regex the backend enforces, so the user sees the rule before a round trip. */
const PASSWORD_RE = /^(?=.*[A-Z])(?=.*\d).{8,}$/;

/**
 * Shared by the admin change-password screen and the cashier profile page —
 * one implementation, so the two cannot drift apart.
 */
export function ChangePasswordForm({ onDone }: { onDone?: () => void }) {
  const t = useTranslations("auth");
  const tc = useTranslations("common");
  const apiError = useApiError();

  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  const change = useMutation({
    mutationFn: () =>
      post("/auth/change-password", { currentPassword: current, newPassword: next }),
    onSuccess: () => {
      setCurrent("");
      setNext("");
      setConfirm("");
      setError(null);
      setDone(true);
      onDone?.();
    },
    onError: (e) => {
      setError(apiError(e, "changePassword"));
      setDone(false);
    },
  });

  function submit() {
    const errors: Record<string, string> = {};
    if (!current) errors.current = tc("required");
    if (!PASSWORD_RE.test(next)) {
      errors.next = t("errPasswordWeak");
    }
    if (next !== confirm) {
      errors.confirm = t("errPasswordMismatch");
    }
    if (next && current === next) {
      errors.next = t("ruleDiffer");
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setDone(false);
    change.mutate();
  }

  return (
    <>
      {error && <Alert tone="error">{error}</Alert>}
      {done && <Alert tone="success">{t("passwordChanged")}</Alert>}

      <Field
        label={t("currentPassword")}
        htmlFor="cp-current"
        required
        error={fieldErrors.current}
      >
        <Input
          id="cp-current"
          type="password"
          autoComplete="current-password"
          value={current}
          onChange={(e) => setCurrent(e.target.value)}
        />
      </Field>

      <FieldRow>
        <Field
          label={t("newPassword")}
          htmlFor="cp-new"
          required
          error={fieldErrors.next}
        >
          <Input
            id="cp-new"
            type="password"
            autoComplete="new-password"
            value={next}
            onChange={(e) => setNext(e.target.value)}
          />
        </Field>
        <Field label={t("confirm")} htmlFor="cp-confirm" required error={fieldErrors.confirm}>
          <Input
            id="cp-confirm"
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
          />
        </Field>
      </FieldRow>

      <div className="mb-3 rounded-sm bg-ink-100 px-3.5 py-3 text-xs leading-7">
        <b>{t("requirements")}</b>
        <ul className="mt-1 list-disc pl-4.5">
          <li>{t("ruleLength")}</li>
          <li>{t("ruleUpper")}</li>
          <li>{t("ruleDigit")}</li>
          <li>{t("ruleDiffer")}</li>
        </ul>
      </div>

      <div className="flex justify-end">
        <Button variant="admin" onClick={submit} loading={change.isPending}>
          {t("updatePassword")}
        </Button>
      </div>
    </>
  );
}
