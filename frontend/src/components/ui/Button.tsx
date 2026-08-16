import type { ButtonHTMLAttributes, ReactNode } from "react";

type Variant = "primary" | "admin" | "accent" | "danger" | "ghost" | "light";
type Size = "sm" | "md" | "lg";

const VARIANTS: Record<Variant, string> = {
  primary: "bg-teal-600 text-white hover:brightness-110",
  admin: "bg-brand-600 text-white hover:brightness-110",
  accent: "bg-orange-500 text-white hover:bg-orange-600",
  danger: "bg-danger-soft text-white hover:brightness-110",
  // `btn-ghost` is the hook globals.css uses to re-colour this on the teal
  // auth surface, where an ink border would be invisible.
  ghost: "btn-ghost border border-ink-300 text-ink-900 hover:bg-ink-100",
  light: "bg-ink-200 text-ink-900 hover:bg-ink-300",
};

const SIZES: Record<Size, string> = {
  sm: "px-3 py-1.5 text-xs",
  md: "px-4 py-2 text-sm",
  lg: "px-5 py-2.5 text-sm",
};

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  block?: boolean;
  loading?: boolean;
  children: ReactNode;
}

export function Button({
  variant = "primary",
  size = "md",
  block = false,
  loading = false,
  disabled,
  className = "",
  children,
  ...rest
}: Props) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={[
        "inline-flex items-center justify-center gap-2 rounded font-semibold",
        "transition disabled:cursor-not-allowed disabled:opacity-60",
        "focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-500",
        VARIANTS[variant],
        SIZES[size],
        block ? "w-full" : "",
        className,
      ].join(" ")}
    >
      {loading && (
        <span
          aria-hidden
          className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-t-transparent"
        />
      )}
      {children}
    </button>
  );
}
