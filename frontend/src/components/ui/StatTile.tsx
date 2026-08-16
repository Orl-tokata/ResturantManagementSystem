import type { ReactNode } from "react";

type Tone = 1 | 2 | 3 | 4;

/** Matches `.stat--1…4` in the prototype: green, sand, teal, red. */
const TONES: Record<Tone, string> = {
  1: "bg-brand-600 text-white",
  2: "bg-sand-300 text-ink-900",
  3: "bg-teal-600 text-white",
  4: "bg-danger-soft text-white",
};

export function StatTile({
  label,
  value,
  tone = 1,
  icon,
}: {
  label: ReactNode;
  value: ReactNode;
  tone?: Tone;
  icon?: ReactNode;
}) {
  return (
    <div className={`rounded-md p-4 shadow-sm ${TONES[tone]}`}>
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <div className="truncate text-xs opacity-90">{label}</div>
          <div className="font-[family-name:var(--font-num)] text-2xl font-bold">{value}</div>
        </div>
        {icon && <div className="shrink-0 opacity-80">{icon}</div>}
      </div>
    </div>
  );
}

export function StatGrid({ children }: { children: ReactNode }) {
  return (
    <div className="mb-4 grid grid-cols-[repeat(auto-fit,minmax(180px,1fr))] gap-3.5">
      {children}
    </div>
  );
}
