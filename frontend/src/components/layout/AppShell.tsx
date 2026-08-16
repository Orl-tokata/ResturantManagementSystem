"use client";

import { Menu, Settings, X } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, type ReactNode } from "react";
import { Clock } from "@/components/layout/Clock";
import { Sidebar } from "@/components/layout/Sidebar";
import { MENUS, type ShellVariant } from "@/lib/menus";

/** Chrome colour per area — teal for the cashier, green for admin. */
const CHROME: Record<ShellVariant, string> = {
  admin: "var(--color-brand-600)",
  cashier: "var(--color-teal-800)",
};

/**
 * Sidebar + topbar + status bar, the React equivalent of renderShell() in the
 * prototype's proto.js.
 *
 * <p>The POS screen deliberately does not use this — it owns the full viewport.
 */
export function AppShell({
  variant,
  children,
}: {
  variant: ShellVariant;
  children: ReactNode;
}) {
  const pathname = usePathname();
  const [drawerOpen, setDrawerOpen] = useState(false);

  // Close the mobile drawer on any route change, including back/forward, which
  // the links' own onNavigate handler would miss. Adjusting state during render
  // is React's documented alternative to mirroring a prop into an effect.
  const [drawerRoute, setDrawerRoute] = useState(pathname);
  if (pathname !== drawerRoute) {
    setDrawerRoute(pathname);
    if (drawerOpen) setDrawerOpen(false);
  }

  const current = MENUS[variant].find(
    (m) => pathname === m.href || pathname.startsWith(`${m.href}/`),
  );
  const title = current ? `${current.km} · ${current.en}` : "Restaurant Management System";

  const settingsHref = variant === "admin" ? "/admin/settings" : "/cashier/profile";

  return (
    <div
      className="grid h-screen overflow-hidden md:grid-cols-[210px_1fr]"
      style={{ ["--chrome" as string]: CHROME[variant] }}
    >
      {/* --- sidebar: fixed drawer on mobile, column on desktop --- */}
      <div className="hidden md:block">
        <Sidebar variant={variant} />
      </div>

      {drawerOpen && (
        <div className="fixed inset-0 z-40 md:hidden">
          <button
            aria-label="Close menu"
            className="absolute inset-0 bg-black/50"
            onClick={() => setDrawerOpen(false)}
          />
          <div className="absolute inset-y-0 left-0 w-[230px] shadow-2xl">
            <Sidebar variant={variant} onNavigate={() => setDrawerOpen(false)} />
          </div>
        </div>
      )}

      {/* --- main column --- */}
      <div className="flex min-w-0 flex-col overflow-hidden">
        <header className="flex h-13 shrink-0 items-center justify-between gap-3 bg-[var(--chrome)] px-4 py-3 text-white">
          <div className="flex min-w-0 items-center gap-2">
            <button
              type="button"
              className="grid h-8 w-8 place-items-center rounded hover:bg-white/15 md:hidden"
              aria-label={drawerOpen ? "Close menu" : "Open menu"}
              onClick={() => setDrawerOpen((v) => !v)}
            >
              {drawerOpen ? <X size={18} /> : <Menu size={18} />}
            </button>
            <h1 className="truncate text-base font-semibold">{title}</h1>
          </div>

          <div className="flex shrink-0 items-center gap-3 text-xs">
            <span className="hidden sm:inline">
              <Clock mode="full" />
            </span>
            <Link
              href={settingsHref}
              aria-label="Settings"
              className="grid h-8 w-8 place-items-center rounded hover:bg-white/15"
            >
              <Settings size={16} />
            </Link>
          </div>
        </header>

        <main className="flex-1 overflow-auto bg-white p-4 md:p-5">{children}</main>

        <footer className="flex h-8 shrink-0 items-center justify-between bg-[var(--chrome)] px-4 text-xs text-white/90">
          <span>Restaurant Management System</span>
          <Clock mode="date" />
        </footer>
      </div>
    </div>
  );
}
