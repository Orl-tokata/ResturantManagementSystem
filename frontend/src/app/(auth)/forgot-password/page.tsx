"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { AuthCard } from "@/components/ui/AuthCard";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input } from "@/components/ui/Field";
import { post } from "@/lib/api";
import { errorMessage } from "@/lib/errors";

export default function ForgotPasswordPage() {
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
    <AuthCard icon="🔒" title="ភ្លេចពាក្យសម្ងាត់" subtitle="Forgot your password?">
      {error && <Alert tone="error">{error}</Alert>}

      <p className="mb-4 text-sm leading-relaxed text-white/85">
        សូមបញ្ចូលអ៊ីមែលរបស់អ្នក។ យើងនឹងផ្ញើលេខកូដបញ្ជាក់ ៦ ខ្ទង់ទៅកាន់អ៊ីមែលនោះ។
        <br />
        <span className="text-white/60">
          Enter your email and we&apos;ll send a 6-digit verification code.
        </span>
      </p>

      <form onSubmit={onSubmit} noValidate>
        <Field label="អ៊ីមែល · Email" htmlFor="email">
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
            ផ្ញើលេខកូដ · Send code
          </Button>
          <Button
            type="button"
            variant="ghost"
            block
            onClick={() => router.push("/login")}
          >
            ត្រឡប់ក្រោយ · Back
          </Button>
        </div>
      </form>
    </AuthCard>
  );
}
