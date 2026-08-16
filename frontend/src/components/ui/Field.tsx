import type { InputHTMLAttributes, ReactNode, SelectHTMLAttributes } from "react";

const CONTROL =
  "w-full rounded border border-transparent bg-white/95 px-3 py-2 text-sm text-ink-900 " +
  "placeholder:text-ink-500 focus:border-teal-500 focus:outline-none " +
  "focus:ring-2 focus:ring-teal-500/30 disabled:opacity-60";

export function Field({
  label,
  htmlFor,
  error,
  hint,
  children,
}: {
  label: ReactNode;
  htmlFor?: string;
  error?: string;
  hint?: ReactNode;
  children: ReactNode;
}) {
  return (
    <div className="mb-3.5">
      <label htmlFor={htmlFor} className="mb-1.5 block text-sm font-semibold text-white/90">
        {label}
      </label>
      {children}
      {hint && !error && <p className="mt-1 text-xs text-white/60">{hint}</p>}
      {error && (
        <p role="alert" className="mt-1 text-xs font-semibold text-orange-500">
          {error}
        </p>
      )}
    </div>
  );
}

export function Input({ className = "", ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...rest} className={`${CONTROL} ${className}`} />;
}

export function Select({
  className = "",
  children,
  ...rest
}: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select {...rest} className={`${CONTROL} ${className}`}>
      {children}
    </select>
  );
}
