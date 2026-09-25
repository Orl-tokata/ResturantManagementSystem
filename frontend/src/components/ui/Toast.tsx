"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";

/**
 * Transient messages, anchored to the viewport.
 *
 * <p>These exist for two jobs and no others:
 *
 * <ul>
 *   <li>confirming an action whose result is not visible — saving settings
 *       changes nothing on screen, so without a word the click looks ignored;
 *   <li>reporting a failure the reader would otherwise never see. Page-level
 *       errors render at the top of the document, so a delete that fails at the
 *       bottom of a long table announces itself somewhere off screen, and the
 *       row simply does not disappear.
 * </ul>
 *
 * <p>What does <em>not</em> belong here is anything the reader must act on.
 * "The password needs a capital letter" goes beside the password field, inside
 * the dialog they are looking at — moving it to a corner makes them read in one
 * place and fix in another, and then takes the message away while they type.
 * Validation stays inline. See PROJECT-SPEC.md §7.3.
 */

export type ToastTone = "success" | "error" | "info";

interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
}

interface ToastApi {
  /** Something worked, and the screen does not already show it. */
  success: (message: string) => void;
  /** Something failed where the reader might not be looking. */
  error: (message: string) => void;
  info: (message: string) => void;
}

const ToastContext = createContext<ToastApi | null>(null);

/** Long enough to read a sentence; failures linger, since they matter more. */
const LIFETIME: Record<ToastTone, number> = {
  success: 4000,
  info: 5000,
  error: 8000,
};

const TONES: Record<ToastTone, string> = {
  success: "border-l-success bg-white text-ink-900",
  error: "border-l-danger bg-white text-ink-900",
  info: "border-l-teal-600 bg-white text-ink-900",
};

const ICONS: Record<ToastTone, string> = {
  success: "✅",
  error: "⚠️",
  info: "ℹ️",
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const nextId = useRef(0);

  const dismiss = useCallback((id: number) => {
    setToasts((list) => list.filter((t) => t.id !== id));
  }, []);

  const push = useCallback((tone: ToastTone, message: string) => {
    const id = nextId.current++;
    // Three is enough to see a burst without burying the screen; the oldest
    // goes, because the newest is the one describing what just happened.
    setToasts((list) => [...list, { id, tone, message }].slice(-3));
  }, []);

  const api = useMemo<ToastApi>(
    () => ({
      success: (m) => push("success", m),
      error: (m) => push("error", m),
      info: (m) => push("info", m),
    }),
    [push],
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      <ToastStack toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  );
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error("useToast must be used inside <ToastProvider>");
  return ctx;
}

function ToastStack({
  toasts,
  onDismiss,
}: {
  toasts: Toast[];
  onDismiss: (id: number) => void;
}) {
  if (toasts.length === 0) return null;

  return (
    <div
      /*
       * polite, not assertive: these report what already happened. An assertive
       * region interrupts a screen reader mid-sentence, which is right for a
       * fire alarm and wrong for "Settings saved".
       */
      aria-live="polite"
      aria-atomic="false"
      /*
       * Top right, below the 52px app header. Sitting flush with the top
       * would cover the language switcher and the settings link, and an error
       * toast stays for eight seconds — long enough to be in the way. On a
       * phone it spans the width instead, where a corner card would be cramped.
       */
      className="pointer-events-none fixed inset-x-0 top-13 z-50 flex flex-col items-center gap-2 p-4 sm:inset-x-auto sm:right-0 sm:items-end"
    >
      {toasts.map((t) => (
        <ToastRow key={t.id} toast={t} onDismiss={onDismiss} />
      ))}
    </div>
  );
}

function ToastRow({ toast, onDismiss }: { toast: Toast; onDismiss: (id: number) => void }) {
  const [hovered, setHovered] = useState(false);

  useEffect(() => {
    // Hovering holds it open. Someone reaching for a long message should not
    // have it vanish as the cursor arrives.
    if (hovered) return;
    const timer = setTimeout(() => onDismiss(toast.id), LIFETIME[toast.tone]);
    return () => clearTimeout(timer);
  }, [hovered, toast.id, toast.tone, onDismiss]);

  return (
    <div
      role={toast.tone === "error" ? "alert" : "status"}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      className={[
        "pointer-events-auto flex w-full max-w-sm items-start gap-2.5 rounded-md border border-ink-200",
        "border-l-4 px-3.5 py-3 text-sm leading-relaxed shadow-lg",
        "motion-safe:animate-[toast-in_160ms_ease-out]",
        TONES[toast.tone],
      ].join(" ")}
    >
      <span aria-hidden>{ICONS[toast.tone]}</span>
      <div className="flex-1">{toast.message}</div>
      <button
        type="button"
        onClick={() => onDismiss(toast.id)}
        aria-label="Dismiss"
        className="-mr-1 -mt-0.5 shrink-0 rounded px-1 text-ink-400 hover:text-ink-700"
      >
        ✕
      </button>
    </div>
  );
}
