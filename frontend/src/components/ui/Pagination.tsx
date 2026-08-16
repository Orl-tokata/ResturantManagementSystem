"use client";

import { Button } from "@/components/ui/Button";

/**
 * Page controls for the list screens. `page` is zero-based to match Spring
 * Data; the labels shown to the user are one-based.
 */
export function Pagination({
  page,
  totalPages,
  totalElements,
  size,
  onPage,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPage: (page: number) => void;
}) {
  if (totalElements === 0) return null;

  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  // A short window around the current page, so 200 pages do not render 200 buttons.
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const end = Math.min(totalPages, start + 5);
  const window = Array.from({ length: Math.max(0, end - start) }, (_, i) => start + i);

  return (
    <div className="mt-3.5 flex flex-wrap items-center justify-between gap-3">
      <span className="text-sm text-ink-500">
        បង្ហាញ {from}–{to} ក្នុងចំណោម {totalElements} · Showing {from}–{to} of {totalElements}
      </span>

      <div className="flex items-center gap-1.5">
        <Button
          size="sm"
          variant="light"
          disabled={page === 0}
          onClick={() => onPage(page - 1)}
        >
          ‹ មុន
        </Button>

        {window.map((p) => (
          <Button
            key={p}
            size="sm"
            variant={p === page ? "admin" : "light"}
            aria-current={p === page ? "page" : undefined}
            onClick={() => onPage(p)}
          >
            {p + 1}
          </Button>
        ))}

        <Button
          size="sm"
          variant="light"
          disabled={page >= totalPages - 1}
          onClick={() => onPage(page + 1)}
        >
          បន្ទាប់ ›
        </Button>
      </div>
    </div>
  );
}
