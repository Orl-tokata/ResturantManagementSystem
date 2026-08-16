"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState, type FormEvent } from "react";
import { AuthCard } from "@/components/ui/AuthCard";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input } from "@/components/ui/Field";
import { post } from "@/lib/api";
import { errorMessage } from "@/lib/errors";

const PASSWORD_RE = /^(?=.*[A-Z])(?=.*\d).{8,}$/;

function ResetPasswordForm() {
  const router = useRouter();
  const params = useSearchParams();
  const resetToken = params.get("token") ?? "";

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);

    const errors: Record<string, string> = {};
    if (!PASSWORD_RE.test(password)) {
      errors.password = "យ៉ាងតិច ៨ តួ មានអក្សរធំ និងលេខ · 8+ chars, one uppercase, one number";
    }
    if (password !== confirm) {
      errors.confirm = "ពាក្យសម្ងាត់មិនត្រូវគ្នា · Passwords do not match";
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setBusy(true);
    try {
      await post("/auth/reset-password", { resetToken, newPassword: password });
      router.replace("/login?reset=1");
    } catch (e) {
      setError(errorMessage(e, "Could not reset the password"));
      setBusy(false);
    }
  }

  if (!resetToken) {
    return (
      <AuthCard icon="🔑" title="កំណត់ពាក្យសម្ងាត់ថ្មី" subtitle="Set a new password">
        <Alert tone="error">
          តំណភ្ជាប់មិនត្រឹមត្រូវ · This link is missing its reset token. Start again from
          forgot password.
        </Alert>
        <Link href="/forgot-password">
          <Button block>ភ្លេចពាក្យសម្ងាត់ · Forgot password</Button>
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard
      icon="🔑"
      title="កំណត់ពាក្យសម្ងាត់ថ្មី"
      subtitle="Set a new password"
      footer={
        <Link href="/login" className="text-teal-100 underline">
          ត្រឡប់ទៅទំព័រចូល
        </Link>
      }
    >
      {error && <Alert tone="error">{error}</Alert>}

      <form onSubmit={onSubmit} noValidate>
        <Field
          label="ពាក្យសម្ងាត់ថ្មី · New password"
          htmlFor="password"
          error={fieldErrors.password}
        >
          <Input
            id="password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </Field>

        <Field
          label="បញ្ជាក់ពាក្យសម្ងាត់ · Confirm password"
          htmlFor="confirm"
          error={fieldErrors.confirm}
        >
          <Input
            id="confirm"
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
          />
        </Field>

        <ul className="mb-4 list-disc pl-5 text-xs leading-7 text-white/75">
          <li>យ៉ាងតិច ៨ តួអក្សរ · at least 8 characters</li>
          <li>មានអក្សរធំ និងលេខ · one uppercase letter and one number</li>
        </ul>

        <Button type="submit" block size="lg" loading={busy}>
          រក្សាទុក · Save password
        </Button>
      </form>
    </AuthCard>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<div className="text-white/70">Loading…</div>}>
      <ResetPasswordForm />
    </Suspense>
  );
}
