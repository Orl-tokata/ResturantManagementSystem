"use client";

import { useRouter, useSearchParams } from "next/navigation";
import {
  Suspense,
  useEffect,
  useRef,
  useState,
  type ClipboardEvent,
  type FormEvent,
  type KeyboardEvent,
} from "react";
import { useTranslations } from "next-intl";
import { AuthCard } from "@/components/ui/AuthCard";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { post } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import type { VerifyOtpResponse } from "@/types/auth";

const LENGTH = 6;
const RESEND_SECONDS = 60;

function VerifyOtpForm() {
  const t = useTranslations("auth");
  const tc = useTranslations("common");
  const tA11y = useTranslations("a11y");
  const apiError = useApiError();

  const router = useRouter();
  const params = useSearchParams();
  const email = params.get("email") ?? "";

  const [digits, setDigits] = useState<string[]>(Array(LENGTH).fill(""));
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [countdown, setCountdown] = useState(RESEND_SECONDS);

  const inputs = useRef<Array<HTMLInputElement | null>>([]);

  useEffect(() => {
    inputs.current[0]?.focus();
  }, []);

  useEffect(() => {
    if (countdown <= 0) return;
    const timer = setTimeout(() => setCountdown((c) => c - 1), 1000);
    return () => clearTimeout(timer);
  }, [countdown]);

  function setDigit(index: number, value: string) {
    const digit = value.replace(/\D/g, "").slice(-1);
    setDigits((prev) => {
      const next = [...prev];
      next[index] = digit;
      return next;
    });
    if (digit && index < LENGTH - 1) inputs.current[index + 1]?.focus();
  }

  function onKeyDown(index: number, event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Backspace" && !digits[index] && index > 0) {
      inputs.current[index - 1]?.focus();
    }
    if (event.key === "ArrowLeft" && index > 0) inputs.current[index - 1]?.focus();
    if (event.key === "ArrowRight" && index < LENGTH - 1) inputs.current[index + 1]?.focus();
  }

  /** Pasting the whole code should fill every box, not just the first. */
  function onPaste(event: ClipboardEvent<HTMLInputElement>) {
    const pasted = event.clipboardData.getData("text").replace(/\D/g, "").slice(0, LENGTH);
    if (!pasted) return;
    event.preventDefault();
    const next = Array(LENGTH).fill("");
    pasted.split("").forEach((d, i) => (next[i] = d));
    setDigits(next);
    inputs.current[Math.min(pasted.length, LENGTH - 1)]?.focus();
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    const code = digits.join("");
    if (code.length !== LENGTH) {
      setError(t("errOtpIncomplete"));
      return;
    }

    setBusy(true);
    try {
      const data = await post<VerifyOtpResponse>("/auth/verify-otp", { email, code });
      router.replace(`/reset-password?token=${encodeURIComponent(data.resetToken)}`);
    } catch (e) {
      setError(apiError(e, "invalidCode"));
      setBusy(false);
    }
  }

  async function resend() {
    setError(null);
    setNotice(null);
    try {
      await post("/auth/forgot-password", { email });
      setDigits(Array(LENGTH).fill(""));
      inputs.current[0]?.focus();
      setCountdown(RESEND_SECONDS);
      setNotice(t("codeResent"));
    } catch (e) {
      setError(apiError(e, "resendCode"));
    }
  }

  const masked = email.replace(/^(.).*(@.*)$/, "$1***$2");

  return (
    <AuthCard icon="✉️" title={t("otpTitle")} subtitle={t("otpSubtitle")}>
      {error && <Alert tone="error">{error}</Alert>}
      {notice && <Alert tone="success">{notice}</Alert>}

      <p className="mb-1 text-center text-sm leading-relaxed text-white/85">
        {t("otpSentTo")}
        <br />
        <b>{masked || "your email"}</b>
      </p>

      <form onSubmit={onSubmit} noValidate>
        <div className="my-5 flex justify-center gap-2">
          {digits.map((digit, i) => (
            <input
              key={i}
              ref={(el) => {
                inputs.current[i] = el;
              }}
              value={digit}
              onChange={(e) => setDigit(i, e.target.value)}
              onKeyDown={(e) => onKeyDown(i, e)}
              onPaste={onPaste}
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={1}
              aria-label={tA11y("digit", { index: i + 1 })}
              className="h-14 w-11 rounded bg-white/95 text-center text-2xl font-bold text-ink-900
                         focus:outline-2 focus:outline-teal-500"
            />
          ))}
        </div>

        <div className="mb-4 text-center text-xs text-white/80">
          {t("noCode")}{" "}
          {countdown > 0 ? (
            <span className="text-white/50">
              {t("resendIn", { seconds: String(countdown).padStart(2, "0") })}
            </span>
          ) : (
            <button type="button" onClick={resend} className="text-teal-100 underline">
              {t("resend")}
            </button>
          )}
        </div>

        <div className="flex gap-2.5">
          <Button type="submit" block loading={busy}>
            {t("verify")}
          </Button>
          <Button
            type="button"
            variant="ghost"
            block
            onClick={() => router.push("/forgot-password")}
          >
            {tc("back")}
          </Button>
        </div>
      </form>
    </AuthCard>
  );
}

export default function VerifyOtpPage() {
  return (
    <Suspense fallback={<div className="text-white/70">Loading…</div>}>
      <VerifyOtpForm />
    </Suspense>
  );
}
