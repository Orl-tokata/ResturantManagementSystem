import type { ReactNode } from "react";

export function Card({
  title,
  action,
  padded = true,
  className = "",
  children,
}: {
  title?: ReactNode;
  action?: ReactNode;
  padded?: boolean;
  className?: string;
  children: ReactNode;
}) {
  return (
    <section
      className={`overflow-hidden rounded-md border border-ink-200 bg-white shadow-sm ${className}`}
    >
      {title && (
        <header className="flex items-center justify-between gap-3 border-b border-ink-200 px-4 py-3 text-sm font-semibold">
          <span className="min-w-0 truncate">{title}</span>
          {action && <div className="shrink-0">{action}</div>}
        </header>
      )}
      <div className={padded ? "p-4" : ""}>{children}</div>
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
