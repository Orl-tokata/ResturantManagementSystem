"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { AuthCard } from "@/components/ui/AuthCard";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Field, Input, Select } from "@/components/ui/Field";
import { useAuth } from "@/lib/auth-context";
import { errorMessage } from "@/lib/errors";
import type { Role } from "@/types/auth";

/** Mirrors the backend's Bean Validation pattern so we fail fast, client-side. */
const PASSWORD_RE = /^(?=.*[A-Z])(?=.*\d).{8,}$/;

export default function SignupPage() {
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
    if (form.fullName.trim().length < 2) errors.fullName = "សូមបញ្ចូលឈ្មោះ · Name is required";
    if (form.username.trim().length < 3) errors.username = "យ៉ាងតិច ៣ តួអក្សរ · At least 3 characters";
    if (!PASSWORD_RE.test(form.password)) {
      errors.password = "យ៉ាងតិច ៨ តួ មានអក្សរធំ និងលេខ · 8+ chars, one uppercase, one number";
    }
    if (form.password !== form.confirm) {
      errors.confirm = "ពាក្យសម្ងាត់មិនត្រូវគ្នា · Passwords do not match";
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
      setError(errorMessage(e, "Could not create the account"));
      setBusy(false);
    }
  }

  return (
    <AuthCard
      icon="🍽️"
      title="បង្កើតគណនីថ្មី"
      subtitle="Create a new account"
      wide
      footer={
        <>
          មានគណនីរួចហើយ?{" "}
          <Link href="/login" className="text-teal-100 underline">
            ចូលប្រើប្រាស់
          </Link>
        </>
      }
    >
      {error && <Alert tone="error">{error}</Alert>}

      <form onSubmit={onSubmit} noValidate>
        <Field label="ឈ្មោះពេញ · Full name" htmlFor="fullName" error={fieldErrors.fullName}>
          <Input
            id="fullName"
            value={form.fullName}
            onChange={(e) => set("fullName", e.target.value)}
            placeholder="Sok Dara"
          />
        </Field>

        <Field label="ឈ្មោះអ្នកប្រើប្រាស់ · Username" htmlFor="username" error={fieldErrors.username}>
          <Input
            id="username"
            autoComplete="username"
            value={form.username}
            onChange={(e) => set("username", e.target.value)}
          />
        </Field>

        <div className="flex gap-3.5">
          <div className="flex-1">
            <Field label="អ៊ីមែល · Email" htmlFor="email">
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
            <Field label="ទូរស័ព្ទ · Phone" htmlFor="phone">
              <Input
                id="phone"
                value={form.phone}
                onChange={(e) => set("phone", e.target.value)}
                placeholder="012 345 678"
              />
            </Field>
          </div>
        </div>

        <Field label="តួនាទី · Role" htmlFor="role">
          <Select id="role" value={form.role} onChange={(e) => set("role", e.target.value as Role)}>
            <option value="CASHIER">អ្នកគិតលុយ · Cashier</option>
            <option value="WAITER">អ្នករត់តុ · Waiter</option>
            <option value="CHEF">ចុងភៅ · Chef</option>
            <option value="ADMIN">អ្នកគ្រប់គ្រង · Admin</option>
          </Select>
        </Field>

        <div className="flex gap-3.5">
          <div className="flex-1">
            <Field label="ពាក្យសម្ងាត់ · Password" htmlFor="password" error={fieldErrors.password}>
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
            <Field label="បញ្ជាក់ · Confirm" htmlFor="confirm" error={fieldErrors.confirm}>
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
          បង្កើតគណនី · Create account
        </Button>
      </form>
    </AuthCard>
  );
}
