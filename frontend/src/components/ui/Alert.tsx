import type { ReactNode } from "react";

type Tone = "error" | "success" | "info" | "warn";

/**
 * The first three are white on a tint, because this component grew up inside
 * AuthCard, whose background is a dark teal gradient. On the admin screens,
 * which are light, white text on a 15% tint is close to invisible — a known
 * fault, not a style choice, and not fixed here because fixing it properly
 * means giving the dark-background callers a way to keep what they have.
 *
 * <p>`warn` is written for where it is actually used: dark amber on a light
 * amber field, matching the Badge of the same name, and legible on both.
 */
const TONES: Record<Tone, string> = {
  error: "border-danger-soft bg-danger-soft/15 text-white",
  success: "border-success bg-success/15 text-white",
  info: "border-teal-100/50 bg-white/10 text-white",
  warn: "border-[#e6a817] bg-[#fdf0d2] text-[#8a6100]",
};

const ICONS: Record<Tone, string> = {
  error: "⚠️",
  success: "✅",
  info: "ℹ️",
  warn: "⚠️",
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
