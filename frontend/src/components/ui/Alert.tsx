import type { ReactNode } from "react";

type Tone = "error" | "success" | "info";

const TONES: Record<Tone, string> = {
  error: "border-danger-soft bg-danger-soft/15 text-white",
  success: "border-success bg-success/15 text-white",
  info: "border-teal-100/50 bg-white/10 text-white",
};

const ICONS: Record<Tone, string> = {
  error: "⚠️",
  success: "✅",
  info: "ℹ️",
};

export function Alert({ tone = "info", children }: { tone?: Tone; children: ReactNode }) {
  return (
    <div
      role={tone === "error" ? "alert" : "status"}
      className={`mb-4 flex gap-2 rounded border-l-4 px-3 py-2.5 text-sm leading-relaxed ${TONES[tone]}`}
    >
      <span aria-hidden>{ICONS[tone]}</span>
      <div>{children}</div>
    </div>
  );
}
