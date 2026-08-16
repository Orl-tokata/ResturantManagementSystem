"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { useAuth } from "@/lib/auth-context";
import type { Role } from "@/types/auth";

/**
 * Client-side route guard.
 *
 * <p>Deliberately not Next middleware: the access token lives in memory and the
 * refresh cookie is scoped to {@code /api/auth}, so middleware running on the
 * edge cannot tell whether a visitor is signed in. The real enforcement is the
 * backend returning 401 — this only avoids showing a shell that would fail to
 * load its data.
 */
export function RequireAuth({
  children,
  roles,
}: {
  children: ReactNode;
  roles?: Role[];
}) {
  const { user, status } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (status === "unauthenticated") {
      router.replace(`/login?next=${encodeURIComponent(pathname)}`);
    }
  }, [status, pathname, router]);

  if (status === "loading") {
    return (
      <div className="grid min-h-screen place-items-center bg-ink-100">
        <div className="flex items-center gap-3 text-sm text-ink-500">
          <span className="h-4 w-4 animate-spin rounded-full border-2 border-teal-600 border-t-transparent" />
          កំពុងផ្ទុក… · Loading
        </div>
      </div>
    );
  }

  if (status === "unauthenticated") {
    return null; // redirect already scheduled
  }

  if (roles && user && !roles.includes(user.role)) {
    return (
      <div className="grid min-h-screen place-items-center bg-ink-100 p-6">
        <div className="max-w-md rounded-md border border-ink-200 bg-white p-6 text-center shadow-sm">
          <div className="mb-2 text-4xl">🚫</div>
          <h1 className="mb-1 text-lg font-bold">គ្មានសិទ្ធិ · Not permitted</h1>
          <p className="text-sm text-ink-500">
            គណនីរបស់អ្នក ({user.role}) មិនអាចចូលទំព័រនេះបានទេ។
            <br />
            Your account does not have access to this page.
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
