import type { ReactNode } from "react";

export function Card({
  title,
  action,
  padded = true,
  fill = false,
  className = "",
  children,
}: {
  title?: ReactNode;
  action?: ReactNode;
  padded?: boolean;
  /**
   * Grow to fill the remaining height of a flex-column parent, and let a
   * filling child do the same inside it.
   *
   * <p>Needed because height has to be handed down explicitly, one level at a
   * time. A DataTable with `fill` asks its parent for the leftover space; if
   * that parent is a plain Card, there is no flex column and no definite
   * height to divide, so the table quietly collapses to its content and the
   * page starts scrolling again. Both the section and its body have to become
   * flex columns for the request to reach the table.
   */
  fill?: boolean;
  className?: string;
  children: ReactNode;
}) {
  return (
    <section
      className={[
        "overflow-hidden rounded-md border border-ink-200 bg-white shadow-sm",
        fill ? "flex min-h-0 flex-1 flex-col" : "",
        className,
      ].join(" ")}
    >
      {title && (
        <header className="flex shrink-0 items-center justify-between gap-3 border-b border-ink-200 px-4 py-3 text-sm font-semibold">
          <span className="min-w-0 truncate">{title}</span>
          {action && <div className="shrink-0">{action}</div>}
        </header>
      )}
      <div
        className={[
          padded ? "p-4" : "",
          fill ? "flex min-h-0 flex-1 flex-col" : "",
        ].join(" ")}
      >
        {children}
      </div>
    </section>
  );
}

/** Filter bar above a list: actions on the left, search on the right. */
export function Toolbar({ left, right }: { left?: ReactNode; right?: ReactNode }) {
  return (
    <div className="mb-3.5 flex flex-wrap items-center justify-between gap-3">
      <div className="flex flex-wrap items-center gap-2">{left}</div>
      <div className="flex flex-wrap items-center gap-2">{right}</div>
    </div>
  );
}

export function PageTitle({ children }: { children: ReactNode }) {
  return <h2 className="mb-3.5 text-xl font-semibold">{children}</h2>;
}

/**
 * Layout for a screen that is one long list: stat tiles and a toolbar on top,
 * a table that takes whatever height is left, a pager at the bottom.
 *
 * <p>The point is that the table's height is measured, not guessed. Sizing it
 * from the viewport instead means picking a number that has to allow for the
 * tiles, the toolbar and the pager — and those differ from screen to screen, so
 * the number is wrong somewhere: too tall and the whole page scrolls, too short
 * and the table is a letterbox with space below it. Letting flexbox divide up a
 * fixed height gets it exactly right on every screen, and keeps the box the
 * same size whether it holds three rows or thirty.
 *
 * <p>Requires an ancestor with a definite height. AppShell's main is a flex
 * child of an h-screen grid, so it has one; `min-h-0` is what stops a tall
 * table from pushing the column past it — without it, a flex item refuses to
 * shrink below its content and the scrollbar reappears on the page instead of
 * inside the table.
 */
export function ListPage({ children }: { children: ReactNode }) {
  return <div className="flex h-full min-h-0 flex-col">{children}</div>;
}
