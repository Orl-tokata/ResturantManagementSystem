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

/**
 * A timestamp for a printed receipt: `2026-11-09 20:16`.
 *
 * <p>Not `toLocaleString()`, which was producing "9/11/2026, 8:16:00 PM" here.
 * That has two faults on a document someone may take to an accountant. It
 * follows the *browser's* locale rather than the till's, so the same sale
 * prints differently on two machines in the same restaurant; and 9/11 is the
 * ninth of November or the eleventh of September depending on who is reading,
 * with nothing on the slip to say which.
 *
 * <p>Year-month-day is unambiguous in every locale and needs no translation,
 * which is what a bilingual receipt wants. Seconds are dropped — nobody
 * reconciles to the second, and the slip is narrow.
 */
export function formatReceiptDateTime(value: string | Date | null | undefined): string {
  if (!value) return "—";
  const d = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(d.getTime())) return "—";

  const pad = (n: number) => String(n).padStart(2, "0");
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    ` ${pad(d.getHours())}:${pad(d.getMinutes())}`
  );
}

/**
 * A timestamp to the second: `2026-11-09 20:16:42`.
 *
 * <p>Same year-month-day reasoning as the receipt above, and for the same
 * reason — the browser's locale must not decide what a record says.
 *
 * <p>Seconds are kept here, where the receipt drops them. An audit log is read
 * in order, and two changes a few seconds apart are exactly the pair someone is
 * trying to tell apart; showing both as 20:16 would make the log look like it
 * recorded them simultaneously.
 */
export function formatTimestamp(value: string | Date | null | undefined): string {
  if (!value) return "—";
  const d = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(d.getTime())) return "—";

  const pad = (n: number) => String(n).padStart(2, "0");
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    ` ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  );
}
