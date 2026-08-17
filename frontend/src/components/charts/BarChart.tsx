"use client";

import { useState } from "react";

export interface BarPoint {
  label: string;
  /** Second line under the tick, e.g. a weekday. Optional. */
  sublabel?: string;
  value: number;
  /** Extra line in the tooltip, e.g. "3 invoices". */
  detail?: string;
}

/**
 * Single-series bar chart for magnitude over time.
 *
 * Follows the house chart rules:
 * - One series, so no legend — the card title names it.
 * - One hue (brand-600). A single series has no adjacent-pair CVD problem, so
 *   there is no categorical palette to validate here.
 * - Thin marks with 4px rounded tops anchored to the baseline, and a 2px
 *   surface gap between adjacent bars.
 * - Recessive grid and axes; values in ink tokens, never in the series colour.
 * - Labels are selective: only the tallest bar is labelled directly, the rest
 *   on hover. A number over every bar is noise.
 * - Per-mark hover tooltip, with a hit target spanning the full column height
 *   rather than just the drawn bar.
 */
export function BarChart({
  points,
  formatValue,
  height = 200,
  emptyMessage = "គ្មានទិន្នន័យ · No data in this range",
}: {
  points: BarPoint[];
  formatValue: (value: number) => string;
  height?: number;
  emptyMessage?: string;
}) {
  const [hover, setHover] = useState<number | null>(null);

  if (points.length === 0) {
    return (
      <p className="grid place-items-center py-10 text-sm text-ink-500" style={{ height }}>
        {emptyMessage}
      </p>
    );
  }

  const max = Math.max(...points.map((p) => p.value));
  const peak = points.reduce((best, p, i) => (p.value > points[best].value ? i : best), 0);
  // A flat zero series would divide by zero; give it a baseline scale.
  const scale = max > 0 ? max : 1;

  return (
    <div>
      {/* plot area */}
      <div className="relative" style={{ height }}>
        {/* recessive gridlines at 0 / 50 / 100% */}
        {[0, 0.5, 1].map((f) => (
          <div
            key={f}
            aria-hidden
            className="absolute inset-x-0 border-t border-ink-200"
            style={{ bottom: `${f * 100}%` }}
          />
        ))}

        <div className="absolute inset-0 flex items-end gap-0.5">
          {points.map((p, i) => {
            const pct = (p.value / scale) * 100;
            const isPeak = i === peak && max > 0;
            const active = hover === i;

            return (
              <div
                key={`${p.label}-${i}`}
                className="group relative flex h-full flex-1 items-end"
                onMouseEnter={() => setHover(i)}
                onMouseLeave={() => setHover(null)}
                onFocus={() => setHover(i)}
                onBlur={() => setHover(null)}
                tabIndex={0}
                role="img"
                aria-label={`${p.label}: ${formatValue(p.value)}`}
              >
                {/* direct label on the peak only; others appear on hover */}
                {(isPeak || active) && (
                  <span className="pointer-events-none absolute inset-x-0 -top-0.5 text-center text-[10px] font-semibold text-ink-700">
                    {formatValue(p.value)}
                  </span>
                )}

                <div
                  className={`w-full rounded-t bg-brand-600 transition-[filter] ${
                    active ? "brightness-110" : ""
                  }`}
                  style={{ height: `${Math.max(pct, p.value > 0 ? 2 : 0)}%` }}
                />

                {active && (
                  <div className="pointer-events-none absolute bottom-full left-1/2 z-10 mb-2 -translate-x-1/2 whitespace-nowrap rounded bg-ink-900 px-2 py-1.5 text-xs text-white shadow-lg">
                    <div className="font-semibold">{p.label}</div>
                    <div>{formatValue(p.value)}</div>
                    {p.detail && <div className="text-white/70">{p.detail}</div>}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* axis */}
      <div className="mt-1.5 flex gap-0.5 border-t border-ink-300 pt-1.5">
        {points.map((p, i) => (
          <div key={`${p.label}-tick-${i}`} className="flex-1 text-center">
            <div className="truncate text-[10px] text-ink-500">{p.label}</div>
            {p.sublabel && <div className="truncate text-[10px] text-ink-400">{p.sublabel}</div>}
          </div>
        ))}
      </div>
    </div>
  );
}
