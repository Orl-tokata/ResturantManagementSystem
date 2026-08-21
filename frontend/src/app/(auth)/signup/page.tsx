"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { useTranslations } from "next-intl";
import { AuthCard } from "@/components/ui/AuthCard";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input, Select } from "@/components/ui/Field";
import { useAuth } from "@/lib/auth-context";
import { useApiError } from "@/lib/use-api-error";
import type { Role } from "@/types/auth";

/** Mirrors the backend's Bean Validation pattern so we fail fast, client-side. */
const PASSWORD_RE = /^(?=.*[A-Z])(?=.*\d).{8,}$/;

export default function SignupPage() {
  const t = useTranslations("auth");
  const tc = useTranslations("common");
  const tRole = useTranslations("enum.role");
  const apiError = useApiError();

  const router = useRouter();
  const { register } = useAuth();

  const [form, setForm] = useState({
    fullName: "",
    username: "",
    email: "",
    phone: "",
    password: "",
    confirm: "",
    role: "CASHIER" as Role,
  });
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function set<K extends keyof typeof form>(key: K, value: (typeof form)[K]) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function validate() {
    const errors: Record<string, string> = {};
    if (form.fullName.trim().length < 2) errors.fullName = t("errNameRequired");
    if (form.username.trim().length < 3) errors.username = t("errUsernameShort");
    if (!PASSWORD_RE.test(form.password)) {
      errors.password = t("errPasswordWeak");
    }
    if (form.password !== form.confirm) {
      errors.confirm = t("errPasswordMismatch");
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    if (!validate()) return;

    setBusy(true);
    try {
      await register({
        username: form.username.trim(),
        password: form.password,
        fullName: form.fullName.trim(),
        email: form.email.trim() || undefined,
        phone: form.phone.trim() || undefined,
        role: form.role,
      });
      router.replace("/login?registered=1");
    } catch (e) {
      setError(apiError(e, "createAccount"));
      setBusy(false);
    }
  }

  return (
    <AuthCard
      icon="🍽️"
      title={t("signupTitle")}
      subtitle={t("signupSubtitle")}
      wide
      footer={
        <>
          {t("haveAccount")}{" "}
          <Link href="/login" className="text-teal-100 underline">
            {t("signIn")}
          </Link>
        </>
      }
    >
      {error && <Alert tone="error">{error}</Alert>}

      <form onSubmit={onSubmit} noValidate>
        <Field label={t("fullName")} htmlFor="fullName" error={fieldErrors.fullName}>
          <Input
            id="fullName"
            value={form.fullName}
            onChange={(e) => set("fullName", e.target.value)}
            placeholder="Sok Dara"
          />
        </Field>

        <Field label={t("username")} htmlFor="username" error={fieldErrors.username}>
          <Input
            id="username"
            autoComplete="username"
            value={form.username}
            onChange={(e) => set("username", e.target.value)}
          />
        </Field>

        <div className="flex gap-3.5">
          <div className="flex-1">
            <Field label={tc("email")} htmlFor="email">
              <Input
                id="email"
                type="email"
                autoComplete="email"
                value={form.email}
                onChange={(e) => set("email", e.target.value)}
                placeholder="name@example.com"
              />
            </Field>
          </div>
          <div className="flex-1">
            <Field label={tc("phone")} htmlFor="phone">
              <Input
                id="phone"
                value={form.phone}
                onChange={(e) => set("phone", e.target.value)}
                placeholder="012 345 678"
              />
            </Field>
          </div>
        </div>

        <Field label={t("role")} htmlFor="role">
          <Select id="role" value={form.role} onChange={(e) => set("role", e.target.value as Role)}>
            <option value="CASHIER">{tRole("CASHIER")}</option>
            <option value="WAITER">{tRole("WAITER")}</option>
            <option value="CHEF">{tRole("CHEF")}</option>
            <option value="ADMIN">{tRole("ADMIN")}</option>
          </Select>
        </Field>

        <div className="flex gap-3.5">
          <div className="flex-1">
            <Field label={t("password")} htmlFor="password" error={fieldErrors.password}>
              <Input
                id="password"
                type="password"
                autoComplete="new-password"
                value={form.password}
                onChange={(e) => set("password", e.target.value)}
              />
            </Field>
          </div>
          <div className="flex-1">
            <Field label={t("confirm")} htmlFor="confirm" error={fieldErrors.confirm}>
              <Input
                id="confirm"
                type="password"
                autoComplete="new-password"
                value={form.confirm}
                onChange={(e) => set("confirm", e.target.value)}
              />
            </Field>
          </div>
        </div>

        <Button type="submit" block size="lg" loading={busy}>
          {t("createAccount")}
        </Button>
      </form>
    </AuthCard>
  );
}
