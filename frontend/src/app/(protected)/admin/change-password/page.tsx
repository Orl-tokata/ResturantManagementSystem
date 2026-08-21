"use client";

import { ChangePasswordForm } from "@/components/auth/ChangePasswordForm";
import { Card, Field, Input } from "@/components/ui";
import { useAuth } from "@/lib/auth-context";
import { useTranslations } from "next-intl";

export default function AdminChangePasswordPage() {
  const { user } = useAuth();
  const t = useTranslations("profile");
  const tA = useTranslations("auth");

  return (
    <div className="mx-auto max-w-130">
      <Card title={t("changePassword")}>
        <Field label={tA("username")} htmlFor="cp-user">
          <Input id="cp-user" value={user?.username ?? ""} readOnly disabled />
        </Field>

        <ChangePasswordForm />
      </Card>
    </div>
  );
}
