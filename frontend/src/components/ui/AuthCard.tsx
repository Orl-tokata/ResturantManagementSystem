import type { ReactNode } from "react";

/**
 * The card shared by all five auth screens — logo, bilingual title, then the
 * form. Mirrors `.auth__card` in the HTML prototype.
 */
export function AuthCard({
  icon,
  title,
  subtitle,
  wide = false,
  children,
  footer,
}: {
  icon: string;
  title: string;
  subtitle: string;
  wide?: boolean;
  children: ReactNode;
  footer?: ReactNode;
}) {
  return (
    <div
      className={`auth-surface w-full ${wide ? "max-w-[480px]" : "max-w-[420px]"} rounded-lg border border-white/20 bg-teal-800 p-7 text-white shadow-2xl`}
    >
      <div className="mb-5 flex items-center gap-3">
        <div className="grid h-12 w-12 shrink-0 place-items-center rounded bg-white/10 text-2xl">
          {icon}
        </div>
        <div>
          <h1 className="text-base font-bold leading-tight">{title}</h1>
          <p className="text-xs text-white/70">{subtitle}</p>
        </div>
      </div>

      {children}

      {footer && <div className="mt-4 text-center text-xs text-white/80">{footer}</div>}
    </div>
  );
}
