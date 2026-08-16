"use client";

import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { useAuth } from "@/lib/auth-context";

/**
 * Temporary landing for the protected routes. Proves the session survives a
 * reload and that logout works. Replaced by the real shell in milestone 5.
 */
export function SignedInPlaceholder({ area }: { area: string }) {
  const { user, logout } = useAuth();
  const router = useRouter();

  async function onLogout() {
    await logout();
    router.replace("/login");
  }

  return (
    <main className="mx-auto w-full max-w-2xl px-6 py-16">
      <div className="rounded-md border border-ink-200 bg-white shadow-sm">
        <div className="border-b border-ink-200 px-5 py-3 font-semibold">{area}</div>

        <div className="space-y-2 px-5 py-5 text-sm">
          <Row label="ឈ្មោះ · Name">{user?.fullName}</Row>
          <Row label="អ្នកប្រើប្រាស់ · Username">{user?.username}</Row>
          <Row label="តួនាទី · Role">
            <span className="rounded-full bg-brand-200 px-2.5 py-0.5 text-xs font-semibold text-brand-700">
              {user?.role}
            </span>
          </Row>
          <Row label="អ៊ីមែល · Email">{user?.email ?? "—"}</Row>

          <div className="pt-4">
            <Button variant="danger" onClick={onLogout}>
              ចាកចេញ · Log out
            </Button>
          </div>
        </div>
      </div>

      <p className="mt-6 rounded-md border-l-4 border-warning bg-[#fdf8ec] px-5 py-4 text-sm leading-relaxed">
        <b>Milestone 4 checkpoint.</b> You are authenticated and this route is guarded.
        Reload the page — the session survives, because the httpOnly refresh cookie is
        silently exchanged for a new access token on mount. The sidebar, topbar and real
        screens arrive in milestone 5.
      </p>
    </main>
  );
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center">
      <span className="w-44 shrink-0 text-ink-500">{label}</span>
      <span className="font-medium">{children}</span>
    </div>
  );
}
