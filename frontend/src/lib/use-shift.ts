"use client";

import { useQuery } from "@tanstack/react-query";
import { get } from "@/lib/api";
import type { Shift } from "@/types/shift";

/** The query key the shift screen invalidates after opening or closing one. */
export const SHIFT_KEY = ["shift", "current"] as const;

/**
 * The signed-in user's open shift, or null.
 *
 * <p>`data` being null is an ordinary answer, not an error: most of the day a
 * manager has no till open. The endpoint returns 200 with a null body for that
 * reason, so a 404 here would mean something is actually wrong.
 */
export function useOpenShift() {
  return useQuery({
    queryKey: SHIFT_KEY,
    // `?? null`, and the reason is worth a line: the server omits null fields
    // entirely, so "no shift open" arrives as an envelope with no `data` at
    // all and `get` resolves to undefined. React Query rejects an undefined
    // result, which turned the query into an error — and an errored gate
    // lets the POS through. The gate fell open the first time it was asked.
    queryFn: async () => (await get<Shift | null>("/shifts/current")) ?? null,
    // Worth a little staleness: the POS gate reads this on every screen, and a
    // shift does not open or close behind the cashier's back.
    staleTime: 30_000,
  });
}
