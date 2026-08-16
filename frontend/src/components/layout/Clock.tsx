"use client";

import { useEffect, useState } from "react";

type Mode = "time" | "date" | "full";

/**
 * Live clock for the topbar and status bar.
 *
 * <p>Renders nothing until mounted: formatting a date on the server and again
 * in the browser produces different strings and trips hydration.
 */
export function Clock({ mode = "time" }: { mode?: Mode }) {
  const [now, setNow] = useState<Date | null>(null);

  useEffect(() => {
    setNow(new Date());
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  if (!now) return <span suppressHydrationWarning />;

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
