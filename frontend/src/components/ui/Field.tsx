import type {
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from "react";

/**
 * Light-surface styling by default — that is what most screens are.
 *
 * The auth screens sit on teal; rather than threading a `tone` prop through
 * every field, `AuthCard` marks its subtree with `.auth-surface` and a small
 * block in globals.css re-colours labels and controls by cascade.
 */
const CONTROL =
  "w-full rounded border border-ink-300 bg-white px-3 py-2 text-sm text-ink-900 " +
  "placeholder:text-ink-500 focus:border-teal-600 focus:outline-none " +
  "focus:ring-2 focus:ring-teal-600/20 disabled:bg-ink-100 disabled:opacity-70";

export function Field({
  label,
  htmlFor,
  error,
  hint,
  required,
  children,
}: {
  label: ReactNode;
  htmlFor?: string;
  error?: string;
  hint?: ReactNode;
  required?: boolean;
  children: ReactNode;
}) {
  return (
    <div className="mb-3.5">
      <label
        htmlFor={htmlFor}
        className="field-label mb-1.5 block text-sm font-semibold text-ink-700"
      >
        {label}
        {required && <span className="ml-0.5 text-danger-soft">*</span>}
      </label>
      {children}
      {hint && !error && <p className="field-hint mt-1 text-xs text-ink-500">{hint}</p>}
      {error && (
        <p role="alert" className="field-error mt-1 text-xs font-semibold text-danger-soft">
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

export function Textarea({
  className = "",
  ...rest
}: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...rest} className={`${CONTROL} min-h-21 resize-y ${className}`} />;
}

/** Two fields side by side, stacking on narrow screens. */
export function FieldRow({ children }: { children: ReactNode }) {
  return <div className="flex flex-col gap-x-3.5 sm:flex-row [&>*]:flex-1">{children}</div>;
}

export function Checkbox({
  label,
  className = "",
  ...rest
}: InputHTMLAttributes<HTMLInputElement> & { label: ReactNode }) {
  return (
    <label className="check-label inline-flex items-center gap-2 text-sm text-ink-900">
      <input
        type="checkbox"
        {...rest}
        className={`h-4 w-4 accent-teal-600 ${className}`}
      />
      {label}
    </label>
  );
}
