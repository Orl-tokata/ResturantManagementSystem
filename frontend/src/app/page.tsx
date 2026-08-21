"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/lib/auth-context";
import { useTranslations } from "next-intl";
import { HOME_BY_ROLE } from "@/types/auth";

/**
 * Entry point: send the visitor wherever they belong once the silent refresh
 * on mount has settled.
 */
export default function RootPage() {
  const { user, status } = useAuth();
  const tc = useTranslations("common");
  const router = useRouter();

  useEffect(() => {
    if (status === "loading") return;
    router.replace(user ? HOME_BY_ROLE[user.role] : "/login");
  }, [status, user, router]);

  return (
    <div className="grid min-h-screen place-items-center bg-teal-800">
      <div className="flex items-center gap-3 text-sm text-white/80">
        <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/80 border-t-transparent" />
        {tc("loading")}
      </div>
    </div>
  );
}
