"use client";

import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Suspense, useState, type FormEvent } from "react";
import { useTranslations } from "next-intl";
import { AuthCard } from "@/components/ui/AuthCard";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input } from "@/components/ui/Field";
import { useAuth } from "@/lib/auth-context";
import { useApiError } from "@/lib/use-api-error";
import { landingPath } from "@/lib/landing";

function LoginForm() {
  const t = useTranslations("auth");
  const apiError = useApiError();

  const router = useRouter();
  const params = useSearchParams();
  const { login } = useAuth();

  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const reason = params.get("reason");

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const user = await login(username.trim(), password);
      router.replace(landingPath(params.get("next"), user.role));
    } catch (e) {
      setError(apiError(e, "signIn"));
      setBusy(false);
    }
  }

  return (
    <AuthCard
      icon="🍽️"
      title={t("loginTitle")}
      subtitle={t("loginSubtitle")}
    >
      {reason === "expired" && (
        <Alert tone="info">
          {t("sessionExpired")}
        </Alert>
      )}
      {error && <Alert tone="error">{error}</Alert>}

      <form onSubmit={onSubmit} noValidate>
        <Field label={t("username")} htmlFor="username">
          <Input
            id="username"
            name="username"
            autoComplete="username"
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </Field>

        <Field label={t("password")} htmlFor="password">
          <Input
            id="password"
            name="password"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </Field>

        <div className="mb-4 flex items-center justify-between text-xs">
          <label className="flex items-center gap-2 text-white/90">
            <input type="checkbox" defaultChecked />
            {t("rememberMe")}
          </label>
          <Link href="/forgot-password" className="text-teal-100 underline">
            {t("forgotPassword")}
          </Link>
        </div>

        <Button type="submit" block size="lg" loading={busy}>
          {busy ? t("loggingIn") : t("login")}
        </Button>
      </form>
    </AuthCard>
  );
}

export default function LoginPage() {
  // useSearchParams needs a Suspense boundary during prerender.
  return (
    <Suspense fallback={<div className="text-white/70">Loading…</div>}>
      <LoginForm />
    </Suspense>
  );
}
