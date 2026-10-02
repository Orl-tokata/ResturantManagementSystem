"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Store } from "lucide-react";
import { get, post, setAccessToken } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import type { AuthResponse, BranchSummary } from "@/types/auth";

/**
 * Which shop you are working in.
 *
 * <p>P1's entire visible result, and PLAN.md says so: "a branch badge in the
 * header. That is all, and it is correct that it is all." Everything else the
 * package did happens underneath — a signed claim and a filter on every query.
 *
 * <p>With one branch it is a label. It only becomes a control when there is
 * somewhere else to go and the person is allowed to go there, because a menu
 * with one item in it is a thing to click for no reason.
 */
export function BranchBadge() {
  const t = useTranslations("branch");
  const { user, setUser } = useAuth();
  const qc = useQueryClient();
  const [open, setOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  const branches = useQuery({
    queryKey: ["auth", "branches"],
    queryFn: () => get<BranchSummary[]>("/auth/branches"),
    staleTime: 5 * 60_000,
  });

  const options = branches.data ?? [];
  const canSwitch = options.length > 1;

  async function switchTo(branchId: number) {
    setBusy(true);
    try {
      const auth = await post<AuthResponse>("/auth/switch-branch", { branchId });
      setAccessToken(auth.accessToken);
      // Everything already fetched belongs to the shop just left, so none of
      // it is still true. Clearing beats invalidating: an invalidated query
      // shows its stale data while it refetches, and here that is another
      // branch's business on screen.
      qc.clear();
      setUser(auth.user);
      setOpen(false);
    } finally {
      setBusy(false);
    }
  }

  const label = user?.branchName ?? t("unknown");

  if (!canSwitch) {
    return (
      <span
        className="hidden items-center gap-1.5 rounded bg-white/15 px-2 py-1 sm:inline-flex"
        title={t("current", { name: label })}
      >
        <Store size={13} aria-hidden />
        <span className="max-w-32 truncate">{label}</span>
      </span>
    );
  }

  return (
    <div className="relative">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="inline-flex items-center gap-1.5 rounded bg-white/15 px-2 py-1 hover:bg-white/25"
        aria-haspopup="menu"
        aria-expanded={open}
        aria-label={t("current", { name: label })}
      >
        <Store size={13} aria-hidden />
        <span className="max-w-32 truncate">{label}</span>
      </button>

      {open && (
        <ul
          role="menu"
          className="absolute right-0 z-30 mt-1 min-w-48 overflow-hidden rounded-md border border-ink-200 bg-white py-1 text-ink-900 shadow-lg"
        >
          {options.map((b) => (
            <li key={b.id}>
              <button
                type="button"
                role="menuitem"
                disabled={busy || b.current}
                onClick={() => void switchTo(b.id)}
                className={`flex w-full items-center justify-between gap-3 px-3 py-2 text-left text-sm hover:bg-ink-100 disabled:opacity-60 ${
                  b.current ? "font-semibold" : ""
                }`}
              >
                <span className="truncate">{b.name}</span>
                <span className="text-xs text-ink-500">{b.code}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
