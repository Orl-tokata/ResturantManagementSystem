"use client";

import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { UserRound } from "lucide-react";
import { ChangePasswordForm } from "@/components/auth/ChangePasswordForm";
import { Alert, Badge, Button, Card, Field, FieldRow, Input, useToast } from "@/components/ui";
import { get, put } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import { useAuth } from "@/lib/auth-context";
import type { User } from "@/types/auth";


/**
 * Someone's own account details.
 *
 * <p>One implementation behind two routes. It began as a cashier screen, and
 * the admin shell simply never got one — so an administrator could not change
 * their own email, which is how a password reset ended up being sent to
 * admin@rms.local and bouncing. The account most likely to need a reset was the
 * one that could not set a deliverable address for it.
 */
export function ProfileScreen() {
  const t = useTranslations("profile");
  const tc = useTranslations("common");
  const tA = useTranslations("auth");
  const tRole = useTranslations("enum.role");
  const apiError = useApiError();
  const toast = useToast();

  const { user, status, setUser } = useAuth();

  const [draft, setDraft] = useState({ fullName: "", email: "", phone: "" });
  const [error, setError] = useState<string | null>(null);

  /*
   * Reads the account from the server rather than from the session.
   *
   * `user` is the snapshot taken at sign-in, and this form submits all three
   * fields together — so seeding from it meant that an account changed
   * anywhere else since that sign-in got silently reverted by whoever next
   * edited their phone number. It happened to this database: the audit log
   * records {"eml":"orltokata@gmail.com"} -> {"eml":"admin@rms.local"} on a
   * save whose real intent was to add a telephone number.
   */
  const account = useQuery({
    queryKey: ["auth", "me"],
    queryFn: () => get<User>("/auth/me"),
    enabled: status === "authenticated",
    staleTime: 0,
  });

  const current = account.data ?? user;

  // Seeded during render rather than mirrored from an effect, so the first
  // paint already carries the values.
  const [seededFor, setSeededFor] = useState<number | null>(null);
  if (account.data && seededFor !== account.data.id) {
    setSeededFor(account.data.id);
    setDraft({
      fullName: account.data.fullName ?? "",
      email: account.data.email ?? "",
      phone: account.data.phone ?? "",
    });
  }

  const save = useMutation({
    mutationFn: () =>
      put<User>("/auth/me", {
        fullName: draft.fullName,
        email: draft.email || undefined,
        phone: draft.phone || undefined,
      }),
    onSuccess: (saved) => {
      // The sidebar and the header read the cached account; without this they
      // keep showing the old name until the next sign-in.
      setUser(saved);
      void account.refetch();
      toast.success(t("saved"));
      setError(null);
    },
    onError: (e) => {
      setError(apiError(e, "saveProfile"));
    },
  });

  function set<K extends keyof typeof draft>(key: K, value: string) {
    setDraft((d) => ({ ...d, [key]: value }));
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
            {current ? tRole(current.role) : ""}
          </div>
        </div>

        <hr className="my-4 border-ink-200" />

        <dl className="space-y-2 text-sm">
          <Row label={tA("username")} value={user?.username ?? "—"} />
          <Row label={tA("role")} value={current ? tRole(current.role) : "—"} />
          <Row
            label={tc("status")}
            value={<Badge tone={user?.locked ? "dead" : "ok"}>{user?.locked ? t("locked") : t("active")}</Badge>}
          />
          <Row
            label={t("lastLogin")}
            value={current?.lastLoginAt ? new Date(current.lastLoginAt).toLocaleString() : "—"}
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
