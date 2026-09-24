"use client";

import { X } from "lucide-react";
import { useTranslations } from "next-intl";
import { useEffect, useId, useRef, type ReactNode } from "react";

type Chrome = "admin" | "cashier" | "danger" | "success";

const CHROME: Record<Chrome, string> = {
  admin: "bg-brand-600",
  cashier: "bg-teal-800",
  danger: "bg-danger-soft",
  success: "bg-success",
};

const WIDTHS = {
  sm: "max-w-[400px]",
  md: "max-w-[520px]",
  lg: "max-w-[700px]",
} as const;

export function Modal({
  open,
  onClose,
  title,
  chrome = "admin",
  width = "md",
  footer,
  children,
}: {
  open: boolean;
  onClose: () => void;
  title: ReactNode;
  chrome?: Chrome;
  width?: keyof typeof WIDTHS;
  footer?: ReactNode;
  children: ReactNode;
}) {
  const t = useTranslations("common");
  const titleId = useId();
  const panelRef = useRef<HTMLDivElement>(null);
  const returnFocusTo = useRef<HTMLElement | null>(null);

  /*
   * Focus and scroll lock, keyed on `open` alone.
   *
   * These used to share an effect with the key handler below, which depends on
   * `onClose`. Every caller passes an inline arrow — onClose={() => setEditing(null)}
   * — so `onClose` is a new function on every render, and every keystroke in a
   * dialog field therefore re-ran the whole effect: the cleanup returned focus
   * to whatever opened the dialog, then the effect moved it to the panel. Either
   * way it left the input, and typing a name meant clicking back after each
   * character.
   *
   * Splitting them is the fix. Re-binding a keydown listener on every render is
   * harmless; moving focus on every render is not.
   */
  useEffect(() => {
    if (!open) return;

    // Remember what had focus so it can be restored on close — otherwise focus
    // jumps to the top of the document and keyboard users lose their place.
    returnFocusTo.current = document.activeElement as HTMLElement | null;
    panelRef.current?.focus();

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousOverflow;
      returnFocusTo.current?.focus?.();
    };
  }, [open]);

  // Escape to close, Tab kept inside. Free to re-bind whenever onClose changes.
  useEffect(() => {
    if (!open) return;

    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        event.stopPropagation();
        onClose();
        return;
      }

      if (event.key !== "Tab" || !panelRef.current) return;
      const focusable = panelRef.current.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])',
      );
      if (focusable.length === 0) return;

      const first = focusable[0];
      const last = focusable[focusable.length - 1];

      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 grid place-items-center bg-black/45 p-4"
      onMouseDown={(e) => {
        // mousedown, not click: a drag that starts inside and ends on the
        // backdrop should not close the dialog.
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        className={`w-full ${WIDTHS[width]} overflow-hidden rounded-md bg-white shadow-2xl outline-none`}
      >
        <header
          className={`flex items-center justify-between gap-3 px-4 py-3 font-semibold text-white ${CHROME[chrome]}`}
        >
          <span id={titleId} className="min-w-0 truncate">
            {title}
          </span>
          <button
            type="button"
            onClick={onClose}
            aria-label={t("close")}
            className="grid h-7 w-7 shrink-0 place-items-center rounded hover:bg-white/20"
          >
            <X size={16} />
          </button>
        </header>

        <div className="max-h-[70vh] overflow-y-auto px-4 py-4">{children}</div>

        {footer && (
          <footer className="flex justify-end gap-2.5 border-t border-ink-200 bg-ink-50 px-4 py-3">
            {footer}
          </footer>
        )}
      </div>
    </div>
  );
}
