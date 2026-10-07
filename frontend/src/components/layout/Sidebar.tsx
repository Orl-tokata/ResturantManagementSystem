"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronDown, LogOut, UserRound } from "lucide-react";
import { useState } from "react";
import { useTranslations } from "next-intl";
import {
  ADMIN_ACCOUNT_MENU,
  ADMIN_GROUPS,
  MENUS,
  activeGroup,
  type MenuItem,
  type ShellVariant,
} from "@/lib/menus";
import { useAuth } from "@/lib/auth-context";

/**
 * The navigation.
 *
 * <p>The admin side is five collapsible headings — SCREENS §2.2, written when
 * the flat list was eleven items and built when it had reached seventeen. A
 * scroll is a menu nobody reads.
 *
 * <p>The cashier side stays flat. Seven items, learnt in a day, and a heading
 * to open before reaching the till would be friction on the most-used screen
 * in the building.
 */
export function Sidebar({
  variant,
  onNavigate,
}: {
  variant: ShellVariant;
  onNavigate?: () => void;
}) {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const t = useTranslations("nav");
  const tGroup = useTranslations("nav.group");
  const tEnum = useTranslations("enum.role");

  /*
   * Which heading is open: the one you are inside, until you say otherwise.
   *
   * Derived from the route rather than remembered, so arriving anywhere opens
   * the right section with nothing stored and nothing to go stale. An
   * override lives as long as the sidebar does, which is what "collapse all
   * but the open group" means in practice.
   */
  const [opened, setOpened] = useState<string | null>(null);
  const current = activeGroup(pathname)?.key;
  const openKey = opened ?? current ?? ADMIN_GROUPS[0].key;

  function isActive(item: MenuItem) {
    // Exact match for a section root, prefix match for its children —
    // otherwise "/admin" would light up on every admin page.
    return (
      pathname === item.href ||
      (item.href !== "/admin" &&
        item.href !== "/cashier" &&
        pathname.startsWith(`${item.href}/`))
    );
  }

  function link(item: MenuItem, inset: boolean) {
    const Icon = item.icon;
    const active = isActive(item);
    return (
      <Link
        key={item.href}
        href={item.href}
        onClick={onNavigate}
        aria-current={active ? "page" : undefined}
        className={[
          "flex items-center gap-2.5 border-l-[3px] py-2.5 text-sm transition",
          inset ? "pl-7 pr-4" : "px-4",
          active
            ? "border-orange-500 bg-black/20 font-semibold text-white"
            : "border-transparent text-white hover:bg-black/15",
        ].join(" ")}
      >
        <Icon size={17} className="shrink-0" />
        <span className="truncate">{t(item.key)}</span>
      </Link>
    );
  }

  return (
    <aside className="flex h-full flex-col overflow-hidden bg-[var(--chrome)] text-white">
      {/* who is signed in */}
      <div className="flex items-center gap-2.5 border-b border-white/15 px-3.5 py-4">
        <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-white/20">
          <UserRound size={18} />
        </div>
        <div className="min-w-0">
          <div className="truncate text-sm font-semibold">{user?.fullName ?? "—"}</div>
          <div className="truncate text-xs text-white/95">{user ? tEnum(user.role) : ""}</div>
        </div>
      </div>

      <nav className="scroll-invert flex-1 overflow-y-auto py-2">
        {variant === "cashier"
          ? MENUS.cashier.map((item) => link(item, false))
          : ADMIN_GROUPS.map((group) => {
              const open = group.key === openKey;
              // A closed heading still shows when the page you are on is
              // inside it, so collapsing does not lose your place.
              const holdsCurrent = group.key === current;

              return (
                <div key={group.key}>
                  <button
                    type="button"
                    onClick={() => setOpened(open ? "" : group.key)}
                    aria-expanded={open}
                    className={[
                      "flex w-full items-center justify-between gap-2 px-4 py-2 text-left",
                      "text-[11px] font-semibold uppercase tracking-wide transition",
                      holdsCurrent ? "text-white" : "text-white/95 hover:text-white",
                    ].join(" ")}
                  >
                    <span className="truncate">{tGroup(group.key)}</span>
                    <ChevronDown
                      size={14}
                      aria-hidden
                      className={`shrink-0 transition-transform ${open ? "" : "-rotate-90"}`}
                    />
                  </button>

                  {open && group.items.map((item) => link(item, true))}
                </div>
              );
            })}

        {variant === "admin" && (
          <div className="mt-2 border-t border-white/15 pt-2">
            {ADMIN_ACCOUNT_MENU.map((item) => link(item, false))}
          </div>
        )}
      </nav>

      {/*
        The button carries the padding, not the row. With it on the row the
        clickable box was 66x16 inside a strip twice that tall, so a click on
        the obvious place to click did nothing.
      */}
      <div className="border-t border-white/15 px-2.5 py-1.5">
        <button
          type="button"
          onClick={() => logout()}
          className="flex w-full items-center gap-2 rounded px-2 py-2.5 text-xs text-white/95 transition hover:bg-black/15 hover:text-white"
        >
          <LogOut size={15} />
          {t("logout")}
        </button>
      </div>
    </aside>
  );
}
