"use client";

import { useSyncExternalStore } from "react";

type Mode = "time" | "date" | "full";

/* ---------------------------------------------------------------------------
   Time is an external, mutable source, so it is read through
   useSyncExternalStore rather than mirrored into state from an effect.

   The snapshot is cached at module level and refreshed only inside the tick.
   Returning `Date.now()` straight from getSnapshot would hand React a new value
   on every render and spin forever.

   getServerSnapshot returns null so the server renders nothing; React then
   swaps in the client value after hydration, which avoids the mismatch that a
   server-rendered timestamp always causes.
   ------------------------------------------------------------------------- */

let snapshot = 0;
const listeners = new Set<() => void>();
let timer: ReturnType<typeof setInterval> | null = null;

function subscribe(listener: () => void) {
  listeners.add(listener);

  // One interval shared by every Clock on the page.
  timer ??= setInterval(() => {
    snapshot = Date.now();
    listeners.forEach((l) => l());
  }, 1000);

  return () => {
    listeners.delete(listener);
    if (listeners.size === 0 && timer) {
      clearInterval(timer);
      timer = null;
    }
  };
}

function getSnapshot() {
  if (snapshot === 0) snapshot = Date.now();
  return snapshot;
}

function getServerSnapshot(): number | null {
  return null;
}

export function Clock({ mode = "time" }: { mode?: Mode }) {
  const value = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot);

  if (value === null) return <span suppressHydrationWarning />;

  const now = new Date(value);
  const time = now.toLocaleTimeString(undefined, {
    hour: "numeric",
    minute: "2-digit",
    second: "2-digit",
  });
  const date = now.toLocaleDateString();

  return (
    <span suppressHydrationWarning>
      {mode === "time" ? time : mode === "date" ? date : `${date}  ${time}`}
    </span>
  );
}
