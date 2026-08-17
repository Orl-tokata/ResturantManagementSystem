"use client";

import { Search, X } from "lucide-react";
import { useTranslations } from "next-intl";
import { useEffect, useRef, useState } from "react";

/**
 * Debounced search input: calls `onChange` 300ms after typing stops, so a list
 * screen does not fire a request per keystroke.
 */
export function SearchBar({
  value,
  onChange,
  placeholder,
  delay = 300,
  className = "",
}: {
  value: string;
  onChange: (value: string) => void;
  /** Defaults to the translated "search" string. */
  placeholder?: string;
  delay?: number;
  className?: string;
}) {
  const t = useTranslations("common");
  const label = placeholder ?? t("search");
  const [local, setLocal] = useState(value);

  // Adjusting state during render — React's documented alternative to syncing
  // a prop into state from an effect. Fires only when the parent genuinely
  // changes `value` (a filter reset), not when it echoes back what we sent.
  const [lastValue, setLastValue] = useState(value);
  if (value !== lastValue) {
    setLastValue(value);
    setLocal(value);
  }

  // Held in a ref so a new inline `onChange` from the parent does not restart
  // the debounce timer mid-typing.
  const onChangeRef = useRef(onChange);
  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  const committed = useRef(value);
  useEffect(() => {
    if (local === committed.current) return;
    const id = setTimeout(() => {
      committed.current = local;
      onChangeRef.current(local);
    }, delay);
    return () => clearTimeout(id);
  }, [local, delay]);

  function clear() {
    setLocal("");
    committed.current = "";
    onChange("");
  }

  return (
    <div
      className={`flex max-w-80 items-center gap-2 rounded border border-ink-300 bg-white px-3 py-1.5 focus-within:border-teal-600 ${className}`}
    >
      <Search size={15} className="shrink-0 text-ink-500" />
      <input
        type="search"
        value={local}
        onChange={(e) => setLocal(e.target.value)}
        placeholder={label}
        aria-label={label}
        className="w-full bg-transparent text-sm outline-none placeholder:text-ink-500"
      />
      {local && (
        <button
          type="button"
          aria-label="Clear search"
          onClick={clear}
          className="shrink-0 text-ink-500 hover:text-ink-900"
        >
          <X size={14} />
        </button>
      )}
    </div>
  );
}
