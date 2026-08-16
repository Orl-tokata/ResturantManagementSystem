"use client";

import type { ReactNode } from "react";

export interface TabItem {
  id: string;
  label: ReactNode;
}

export function Tabs({
  items,
  active,
  onChange,
}: {
  items: TabItem[];
  active: string;
  onChange: (id: string) => void;
}) {
  return (
    <div role="tablist" className="mb-4 flex flex-wrap gap-1 border-b-2 border-ink-200">
      {items.map((item) => {
        const selected = item.id === active;
        return (
          <button
            key={item.id}
            role="tab"
            type="button"
            aria-selected={selected}
            onClick={() => onChange(item.id)}
            className={[
              "-mb-0.5 border-b-2 px-4 py-2.5 text-sm font-semibold transition",
              selected
                ? "border-brand-600 text-brand-600"
                : "border-transparent text-ink-500 hover:text-ink-900",
            ].join(" ")}
          >
            {item.label}
          </button>
        );
      })}
    </div>
  );
}
