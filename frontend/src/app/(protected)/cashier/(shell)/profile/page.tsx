"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { UserRound } from "lucide-react";
import { ChangePasswordForm } from "@/components/auth/ChangePasswordForm";
import { Alert, Badge, Button, Card, Field, FieldRow, Input } from "@/components/ui";
import { put } from "@/lib/api";
import { errorMessage } from "@/lib/errors";
import { useAuth } from "@/lib/auth-context";
import type { User } from "@/types/auth";


export default function CashierProfilePage() {
  const t = useTranslations("profile");
  const tc = useTranslations("common");
  const tA = useTranslations("auth");
  const tRole = useTranslations("enum.role");

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
      setError(t("errName"));
      return;
    }
    save.mutate();
  }

  if (status === "loading") {
    return <p className="text-sm text-ink-500">{tc("loading")}</p>;
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
            {user ? tRole(user.role) : ""}
          </div>
        </div>

        <hr className="my-4 border-ink-200" />

        <dl className="space-y-2 text-sm">
          <Row label={tA("username")} value={user?.username ?? "—"} />
          <Row label={tA("role")} value={user ? tRole(user.role) : "—"} />
          <Row
            label={tc("status")}
            value={<Badge tone={user?.locked ? "dead" : "ok"}>{user?.locked ? t("locked") : t("active")}</Badge>}
          />
          <Row
            label={t("lastLogin")}
            value={user?.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : "—"}
          />
        </dl>

        <p className="mt-4 text-xs text-ink-500">
          {t("adminOnlyNote")}
        </p>
      </Card>

      {/* ---- editable fields ---- */}
      <div className="space-y-4">
        <Card title={t("general")}>
          {error && <Alert tone="error">{error}</Alert>}
          {saved && <Alert tone="success">{t("saved")}</Alert>}

          <Field label={tA("fullName")} htmlFor="p-name" required>
            <Input
              id="p-name"
              value={draft.fullName}
              onChange={(e) => set("fullName", e.target.value)}
            />
          </Field>

          <FieldRow>
            <Field label={tc("email")} htmlFor="p-email">
              <Input
                id="p-email"
                type="email"
                value={draft.email}
                onChange={(e) => set("email", e.target.value)}
              />
            </Field>
            <Field label={tc("phone")} htmlFor="p-phone">
              <Input
                id="p-phone"
                value={draft.phone}
                onChange={(e) => set("phone", e.target.value)}
              />
            </Field>
          </FieldRow>

          <div className="flex justify-end">
            <Button variant="primary" onClick={submit} loading={save.isPending}>
              {tc("save")}
            </Button>
          </div>
        </Card>

        <Card title={t("changePassword")}>
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
