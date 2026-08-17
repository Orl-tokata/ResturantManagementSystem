"use client";

import { ChangePasswordForm } from "@/components/auth/ChangePasswordForm";
import { Card, Field, Input } from "@/components/ui";
import { useAuth } from "@/lib/auth-context";

export default function AdminChangePasswordPage() {
  const { user } = useAuth();

  return (
    <div className="mx-auto max-w-130">
      <Card title="ប្តូរពាក្យសម្ងាត់ · Change password">
        <Field label="ឈ្មោះអ្នកប្រើប្រាស់ · Username" htmlFor="cp-user">
          <Input id="cp-user" value={user?.username ?? ""} readOnly disabled />
        </Field>

        <ChangePasswordForm />
      </Card>
    </div>
  );
}
