import type { ReactNode } from "react";

export type BadgeTone = "ok" | "warn" | "dead" | "info" | "neutral";

const TONES: Record<BadgeTone, string> = {
  ok: "bg-[#dcf3e6] text-[#1c6b3d]",
  warn: "bg-[#fdf0d2] text-[#8a6100]",
  dead: "bg-[#fbdcdc] text-[#8f1414]",
  info: "bg-[#dbe9f7] text-[#23577f]",
  neutral: "bg-ink-200 text-ink-700",
};

export function Badge({
  tone = "neutral",
  children,
}: {
  tone?: BadgeTone;
  children: ReactNode;
}) {
  return (
    <span
      className={`inline-block whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold ${TONES[tone]}`}
    >
      {children}
    </span>
  );
}

/* ---- Domain status → tone -------------------------------------------------
   Kept here so a status colour is decided once, not re-guessed on each screen.
   -------------------------------------------------------------------------- */

export function toneForOrderStatus(status: string): BadgeTone {
  switch (status) {
    case "PAID":
      return "ok";
    case "OPEN":
      return "warn";
    case "CANCELLED":
      return "dead";
    default:
      return "neutral";
  }
}

export function toneForTableStatus(status: string): BadgeTone {
  switch (status) {
    case "FREE":
      return "ok";
    case "OCCUPIED":
      return "dead";
    case "RESERVED":
      return "warn";
    default:
      return "neutral";
  }
}

export function toneForRecordStatus(status: string): BadgeTone {
  return status === "ACTIVE" ? "ok" : "neutral";
}
