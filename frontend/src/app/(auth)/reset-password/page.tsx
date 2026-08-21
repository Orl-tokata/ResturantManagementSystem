"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Suspense, useState, type FormEvent } from "react";
import { useTranslations } from "next-intl";
import { AuthCard } from "@/components/ui/AuthCard";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input } from "@/components/ui/Field";
import { post } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";

const PASSWORD_RE = /^(?=.*[A-Z])(?=.*\d).{8,}$/;

function ResetPasswordForm() {
  const t = useTranslations("auth");
  const apiError = useApiError();

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
      errors.password = t("errPasswordWeak");
    }
    if (password !== confirm) {
      errors.confirm = t("errPasswordMismatch");
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;

    setBusy(true);
    try {
      await post("/auth/reset-password", { resetToken, newPassword: password });
      router.replace("/login?reset=1");
    } catch (e) {
      setError(apiError(e, "resetPassword"));
      setBusy(false);
    }
  }

  if (!resetToken) {
    return (
      <AuthCard icon="🔑" title={t("resetTitle")} subtitle={t("resetSubtitle")}>
        <Alert tone="error">
          {t("invalidResetLink")}
        </Alert>
        <Link href="/forgot-password">
          <Button block>{t("forgotTitle")}</Button>
        </Link>
      </AuthCard>
    );
  }

  return (
    <AuthCard
      icon="🔑"
      title={t("resetTitle")}
      subtitle={t("resetSubtitle")}
      footer={
        <Link href="/login" className="text-teal-100 underline">
          {t("backToLogin")}
        </Link>
      }
    >
      {error && <Alert tone="error">{error}</Alert>}

      <form onSubmit={onSubmit} noValidate>
        <Field
          label={t("newPassword")}
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
          label={t("confirmPassword")}
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
          <li>{t("ruleLength")}</li>
          <li>{t("ruleUpper")}</li>
        </ul>

        <Button type="submit" block size="lg" loading={busy}>
          {t("savePassword")}
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
