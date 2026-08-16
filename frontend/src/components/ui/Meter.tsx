/**
 * Stock level bar. Colour is derived from the ratio rather than passed in, so
 * "low" means the same thing on every screen.
 */
export function Meter({
  value,
  max,
  className = "",
}: {
  value: number;
  max: number;
  className?: string;
}) {
  const safeMax = max > 0 ? max : 1;
  const ratio = value / safeMax;
  const pct = Math.max(0, Math.min(100, ratio * 100));

  const colour =
    ratio <= 0.15 ? "bg-danger-soft" : ratio < 1 ? "bg-warning" : "bg-success";

  return (
    <div
      role="meter"
      aria-valuenow={value}
      aria-valuemin={0}
      aria-valuemax={safeMax}
      className={`h-[7px] min-w-[90px] overflow-hidden rounded-full bg-ink-200 ${className}`}
    >
      <div className={`h-full rounded-full ${colour}`} style={{ width: `${pct}%` }} />
    </div>
  );
}
