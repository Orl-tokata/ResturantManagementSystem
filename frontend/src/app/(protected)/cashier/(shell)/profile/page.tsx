"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { UserRound } from "lucide-react";
import { ChangePasswordForm } from "@/components/auth/ChangePasswordForm";
import { Alert, Badge, Button, Card, Field, FieldRow, Input } from "@/components/ui";
import { put } from "@/lib/api";
import { errorMessage } from "@/lib/errors";
import { useAuth } from "@/lib/auth-context";
import type { User } from "@/types/auth";
import { ROLE_LABEL } from "@/types/master";

export default function CashierProfilePage() {
  const { user, status } = useAuth();

  const [draft, setDraft] = useState({ fullName: "", email: "", phone: "" });
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Seed from the session once it has loaded, adjusting during render rather
  // than mirroring into state from an effect.
  const [seededFor, setSeededFor] = useState<number | null>(null);
  if (user && seededFor !== user.id) {
    setSeededFor(user.id);
    setDraft({
      fullName: user.fullName ?? "",
      email: user.email ?? "",
      phone: user.phone ?? "",
    });
  }

  const save = useMutation({
    mutationFn: () =>
      put<User>("/auth/me", {
        fullName: draft.fullName,
        email: draft.email || undefined,
        phone: draft.phone || undefined,
      }),
    onSuccess: () => {
      setSaved(true);
      setError(null);
    },
    onError: (e) => {
      setError(errorMessage(e, "Could not save the profile"));
      setSaved(false);
    },
  });

  function set<K extends keyof typeof draft>(key: K, value: string) {
    setDraft((d) => ({ ...d, [key]: value }));
    setSaved(false);
  }

  function submit() {
    if (!draft.fullName.trim()) {
      setError("ឈ្មោះត្រូវការ · Name is required");
      return;
    }
    save.mutate();
  }

  if (status === "loading") {
    return <p className="text-sm text-ink-500">កំពុងផ្ទុក… · Loading</p>;
  }

  return (
    <div className="grid gap-4 lg:grid-cols-[260px_1fr] lg:items-start">
      {/* ---- identity card ---- */}
      <Card>
        <div className="text-center">
          <div className="mx-auto mb-3 grid h-30 w-30 place-items-center rounded-full bg-teal-800 text-white">
            <UserRound size={52} />
          </div>
          <div className="text-lg font-bold">{user?.fullName}</div>
          <div className="text-sm text-ink-500">
            {user ? ROLE_LABEL[user.role] : ""}
          </div>
        </div>

        <hr className="my-4 border-ink-200" />

        <dl className="space-y-2 text-sm">
          <Row label="ឈ្មោះអ្នកប្រើប្រាស់" value={user?.username ?? "—"} />
          <Row label="តួនាទី" value={user ? user.role : "—"} />
          <Row
            label="ស្ថានភាព"
            value={<Badge tone={user?.locked ? "dead" : "ok"}>{user?.locked ? "ជាប់សោ" : "សកម្ម"}</Badge>}
          />
          <Row
            label="ចូលចុងក្រោយ"
            value={user?.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : "—"}
          />
        </dl>

        <p className="mt-4 text-xs text-ink-500">
          តួនាទី និងឈ្មោះអ្នកប្រើប្រាស់ត្រូវកែដោយអ្នកគ្រប់គ្រង · Role and username can only be
          changed by an administrator.
        </p>
      </Card>

      {/* ---- editable fields ---- */}
      <div className="space-y-4">
        <Card title="ព័ត៌មានទូទៅ · General information">
          {error && <Alert tone="error">{error}</Alert>}
          {saved && <Alert tone="success">បានរក្សាទុក · Profile saved</Alert>}

          <Field label="ឈ្មោះពេញ · Full name" htmlFor="p-name" required>
            <Input
              id="p-name"
              value={draft.fullName}
              onChange={(e) => set("fullName", e.target.value)}
            />
          </Field>

          <FieldRow>
            <Field label="អ៊ីមែល · Email" htmlFor="p-email">
              <Input
                id="p-email"
                type="email"
                value={draft.email}
                onChange={(e) => set("email", e.target.value)}
              />
            </Field>
            <Field label="លេខទូរស័ព្ទ · Phone" htmlFor="p-phone">
              <Input
                id="p-phone"
                value={draft.phone}
                onChange={(e) => set("phone", e.target.value)}
              />
            </Field>
          </FieldRow>

          <div className="flex justify-end">
            <Button variant="primary" onClick={submit} loading={save.isPending}>
              រក្សាទុក · Save
            </Button>
          </div>
        </Card>

        <Card title="ប្តូរពាក្យសម្ងាត់ · Change password">
          <ChangePasswordForm />
        </Card>
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-2">
      <dt className="text-ink-500">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  );
}
