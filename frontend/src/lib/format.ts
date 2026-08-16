/**
 * All money formatting goes through here — never inline `toFixed(2)` in a
 * component (PROJECT-SPEC.md §11).
 */

/** Default riel rate. Overridden at runtime by AppSetting `currency.khrRate`. */
export const DEFAULT_KHR_RATE = 4100;

const usd = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
});

const khr = new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 });

export function formatUsd(amount: number | string | null | undefined): string {
  const n = typeof amount === "string" ? Number(amount) : amount;
  if (n == null || Number.isNaN(n)) return "$0.00";
  return usd.format(n);
}

/** Riel has no minor unit — always rendered as a whole number. */
export function formatKhr(
  amountUsd: number | string | null | undefined,
  rate: number = DEFAULT_KHR_RATE,
): string {
  const n = typeof amountUsd === "string" ? Number(amountUsd) : amountUsd;
  if (n == null || Number.isNaN(n)) return "0 ៛";
  return `${khr.format(Math.round(n * rate))} ៛`;
}

export function formatQty(n: number | null | undefined): string {
  if (n == null || Number.isNaN(n)) return "0";
  return new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 }).format(n);
}

/** INV-00147 style. */
export function formatDocNo(prefix: string, seq: number): string {
  return `${prefix}${String(seq).padStart(5, "0")}`;
}
