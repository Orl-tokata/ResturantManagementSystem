import type { ReactNode } from "react";

type Tone = "error" | "success" | "info" | "warn";

/**
 * Dark text on a light tint by default, and white on a translucent tint inside
 * AuthCard.
 *
 * <p>It used to be white everywhere, because the component grew up inside
 * AuthCard and its ground is dark teal. Thirty-one of the thirty-five screens
 * that show an Alert are light, and fifty-six of the calls are
 * {@code tone="error"} — so the usual case was white text on a 15% tint, which
 * is the message a cashier needs most and could barely read.
 *
 * <p>The dark treatment is kept for the auth screens and selected by the
 * {@code .auth-surface} class AuthCard already carries, so no caller passes
 * anything and an Alert added to either kind of screen is right by default.
 *
 * <p>The darker hexes are deliberate: `success` and `info` at their token
 * values clear 3:1 against a tint but not the 4.5:1 that body text wants.
 * `warn` was already written this way and is unchanged.
 */
const TONES: Record<Tone, string> = {
  error:
    "border-danger bg-danger/10 text-danger" +
    " [.auth-surface_&]:border-danger-soft [.auth-surface_&]:bg-danger-soft/15 [.auth-surface_&]:text-white",
  success:
    "border-success bg-success/10 text-success-ink" +
    " [.auth-surface_&]:bg-success/15 [.auth-surface_&]:text-white",
  info:
    "border-info bg-info/10 text-info-ink" +
    " [.auth-surface_&]:border-teal-100/50 [.auth-surface_&]:bg-white/10 [.auth-surface_&]:text-white",
  warn: "border-warning bg-[#fdf0d2] text-warning-ink",
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
