"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { Alert, Button, Field, FieldRow, Input } from "@/components/ui";
import { post } from "@/lib/api";
import { errorMessage } from "@/lib/errors";

/** Same regex the backend enforces, so the user sees the rule before a round trip. */
const PASSWORD_RE = /^(?=.*[A-Z])(?=.*\d).{8,}$/;

/**
 * Shared by the admin change-password screen and the cashier profile page —
 * one implementation, so the two cannot drift apart.
 */
export function ChangePasswordForm({ onDone }: { onDone?: () => void }) {
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
      setError(errorMessage(e, "Could not change the password"));
      setDone(false);
    },
  });

  function submit() {
    const errors: Record<string, string> = {};
    if (!current) errors.current = "ត្រូវការ · Required";
    if (!PASSWORD_RE.test(next)) {
      errors.next = "យ៉ាងតិច ៨ តួ មានអក្សរធំ និងលេខ · 8+ chars, one uppercase, one number";
    }
    if (next !== confirm) {
      errors.confirm = "ពាក្យសម្ងាត់មិនត្រូវគ្នា · Passwords do not match";
    }
    if (next && current === next) {
      errors.next = "ត្រូវខុសពីពាក្យសម្ងាត់ចាស់ · Must differ from the current password";
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setDone(false);
    change.mutate();
  }

  return (
    <>
      {error && <Alert tone="error">{error}</Alert>}
      {done && <Alert tone="success">ពាក្យសម្ងាត់ត្រូវបានប្តូរដោយជោគជ័យ · Password changed</Alert>}

      <Field
        label="ពាក្យសម្ងាត់បច្ចុប្បន្ន · Current password"
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
          label="ពាក្យសម្ងាត់ថ្មី · New password"
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
        <Field label="បញ្ជាក់ · Confirm" htmlFor="cp-confirm" required error={fieldErrors.confirm}>
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
        <b>តម្រូវការពាក្យសម្ងាត់ · Password requirements</b>
        <ul className="mt-1 list-disc pl-4.5">
          <li>យ៉ាងតិច ៨ តួអក្សរ · at least 8 characters</li>
          <li>មានអក្សរធំយ៉ាងតិច ១ · one uppercase letter</li>
          <li>មានលេខយ៉ាងតិច ១ · one number</li>
          <li>មិនដូចពាក្យសម្ងាត់ចាស់ · must differ from the current one</li>
        </ul>
      </div>

      <div className="flex justify-end">
        <Button variant="admin" onClick={submit} loading={change.isPending}>
          ប្តូរពាក្យសម្ងាត់ · Update password
        </Button>
      </div>
    </>
  );
}
