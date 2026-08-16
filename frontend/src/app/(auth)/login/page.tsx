"use client";

import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Suspense, useState, type FormEvent } from "react";
import { AuthCard } from "@/components/ui/AuthCard";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input } from "@/components/ui/Field";
import { useAuth } from "@/lib/auth-context";
import { errorMessage } from "@/lib/errors";
import { HOME_BY_ROLE } from "@/types/auth";

function LoginForm() {
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
      // Honour ?next= only when it is a local path — an absolute URL here
      // would turn the login screen into an open redirect.
      const next = params.get("next");
      const target = next?.startsWith("/") && !next.startsWith("//")
        ? next
        : HOME_BY_ROLE[user.role];
      router.replace(target);
    } catch (e) {
      setError(errorMessage(e, "Could not sign in"));
      setBusy(false);
    }
  }

  return (
    <AuthCard
      icon="🍽️"
      title="កម្មវិធីគ្រប់គ្រងហាងបាយ"
      subtitle="Restaurant Management System"
      footer={
        <>
          មិនទាន់មានគណនី?{" "}
          <Link href="/signup" className="text-teal-100 underline">
            បង្កើតគណនីថ្មី
          </Link>
        </>
      }
    >
      {reason === "expired" && (
        <Alert tone="info">
          វគ្គរបស់អ្នកបានផុតកំណត់ · Your session expired. Please sign in again.
        </Alert>
      )}
      {error && <Alert tone="error">{error}</Alert>}

      <form onSubmit={onSubmit} noValidate>
        <Field label="ឈ្មោះអ្នកប្រើប្រាស់ · Username" htmlFor="username">
          <Input
            id="username"
            name="username"
            autoComplete="username"
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </Field>

        <Field label="ពាក្យសម្ងាត់ · Password" htmlFor="password">
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
            ចងចាំខ្ញុំ · Remember me
          </label>
          <Link href="/forgot-password" className="text-teal-100 underline">
            ភ្លេចពាក្យសម្ងាត់?
          </Link>
        </div>

        <Button type="submit" block size="lg" loading={busy}>
          {busy ? "កំពុងចូល…" : "ចូល · Login"}
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
