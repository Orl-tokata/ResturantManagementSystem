"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { useTranslations } from "next-intl";
import { AuthCard } from "@/components/ui/AuthCard";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input } from "@/components/ui/Field";
import { post } from "@/lib/api";
import { errorMessage } from "@/lib/errors";

export default function ForgotPasswordPage() {
  const t = useTranslations("auth");
  const tc = useTranslations("common");

  const router = useRouter();
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await post("/auth/forgot-password", { email: email.trim() });
      // The backend answers the same way for unknown addresses, so there is
      // nothing to branch on — go straight to the code screen.
      router.push(`/verify-otp?email=${encodeURIComponent(email.trim())}`);
    } catch (e) {
      setError(errorMessage(e, "Could not send the code"));
      setBusy(false);
    }
  }

  return (
    <AuthCard icon="🔒" title={t("forgotTitle")} subtitle={t("forgotSubtitle")}>
      {error && <Alert tone="error">{error}</Alert>}

      <p className="mb-4 text-sm leading-relaxed text-white/85">
        {t("forgotHelp")}
      </p>

      <form onSubmit={onSubmit} noValidate>
        <Field label={tc("email")} htmlFor="email">
          <Input
            id="email"
            type="email"
            autoComplete="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="name@example.com"
          />
        </Field>

        <div className="flex gap-2.5">
          <Button type="submit" block loading={busy}>
            {t("sendCode")}
          </Button>
          <Button
            type="button"
            variant="ghost"
            block
            onClick={() => router.push("/login")}
          >
            {tc("back")}
          </Button>
        </div>
      </form>
    </AuthCard>
  );
}
