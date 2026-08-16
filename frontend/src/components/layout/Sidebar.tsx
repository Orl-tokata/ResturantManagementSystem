"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LogOut, UserRound } from "lucide-react";
import { MENUS, type ShellVariant } from "@/lib/menus";
import { useAuth } from "@/lib/auth-context";

export function Sidebar({
  variant,
  onNavigate,
}: {
  variant: ShellVariant;
  onNavigate?: () => void;
}) {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const items = MENUS[variant];

  return (
    <aside className="flex h-full flex-col overflow-hidden bg-[var(--chrome)] text-white">
      {/* who is signed in */}
      <div className="flex items-center gap-2.5 border-b border-white/15 px-3.5 py-4">
        <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-white/20">
          <UserRound size={18} />
        </div>
        <div className="min-w-0">
          <div className="truncate text-sm font-semibold">{user?.fullName ?? "—"}</div>
          <div className="truncate text-xs text-white/75">{user?.role ?? ""}</div>
        </div>
      </div>

      {/* navigation */}
      <nav className="flex-1 overflow-y-auto py-2">
        {items.map((item) => {
          // Exact match for the section root, prefix match for its children —
          // otherwise "/admin" would light up on every admin page.
          const active =
            pathname === item.href ||
            (item.href !== "/admin" &&
              item.href !== "/cashier" &&
              pathname.startsWith(`${item.href}/`));

          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              onClick={onNavigate}
              aria-current={active ? "page" : undefined}
              className={[
                "flex items-center gap-2.5 border-l-[3px] px-4 py-2.5 text-sm transition",
                active
                  ? "border-orange-500 bg-white/15 font-semibold text-white"
                  : "border-transparent text-white/85 hover:bg-white/10 hover:text-white",
              ].join(" ")}
            >
              <Icon size={17} className="shrink-0" />
              <span className="truncate">
                {item.km} <span className="text-white/60">· {item.en}</span>
              </span>
            </Link>
          );
        })}
      </nav>

      <div className="border-t border-white/15 px-3.5 py-3">
        <button
          type="button"
          onClick={() => logout()}
          className="flex items-center gap-2 text-xs text-white/85 hover:text-white"
        >
          <LogOut size={15} />
          ចាកចេញ · Log out
        </button>
      </div>
    </aside>
  );
}
