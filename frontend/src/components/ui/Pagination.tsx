"use client";

import { useTranslations } from "next-intl";
import { Button } from "@/components/ui/Button";

/**
 * Mirrors Paging.MAX_SIZE on the server. Asking for more is not refused, it is
 * clamped — so "All" is honest about a list longer than this: the pager stays,
 * and the count line still says how many there really are.
 */
export const MAX_PAGE_SIZE = 500;

/** What every list screen starts at. */
export const DEFAULT_PAGE_SIZE = 20;

const SIZE_CHOICES = [20, 50, 100] as const;

/**
 * Page controls for the list screens. `page` is zero-based to match Spring
 * Data; the labels shown to the user are one-based.
 *
 * <p>Renders its row even with nothing to page through, rather than returning
 * null. On a screen where the table above fills the leftover height, a pager
 * that disappears hands its space to the table — so the same list was 361px
 * tall with results and 403px without, and the box changed size as you typed
 * in the search field. The count line is worth keeping anyway: "0 of 0" is an
 * answer, and an empty strip is not.
 */
export function Pagination({
  page,
  totalPages,
  totalElements,
  size,
  onPage,
  onSize,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPage: (page: number) => void;
  /** Omit to keep a screen's page size fixed and hide the chooser. */
  onSize?: (size: number) => void;
}) {
  const t = useTranslations("common");

  if (totalElements === 0) {
    return (
      // Same margin and the same minimum height as the populated row, so the
      // space it occupies does not depend on what is in it.
      <div className="mt-3.5 flex min-h-8 items-center">
        <span className="text-sm text-ink-500">{t("showingNone")}</span>
      </div>
    );
  }

  const from = page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);

  const chooser = onSize ? (
    <label className="flex items-center gap-1.5 text-sm text-ink-500">
      {t("rowsPerPage")}
      <select
        value={size}
        onChange={(e) => onSize(Number(e.target.value))}
        className="rounded border border-ink-300 bg-white px-1.5 py-1 text-sm text-ink-900 focus:border-teal-600 focus:outline-none"
      >
        {SIZE_CHOICES.map((n) => (
          <option key={n} value={n}>
            {n}
          </option>
        ))}
        <option value={MAX_PAGE_SIZE}>{t("allRows")}</option>
      </select>
    </label>
  ) : null;

  // A short window around the current page, so 200 pages do not render 200 buttons.
  const start = Math.max(0, Math.min(page - 2, totalPages - 5));
  const end = Math.min(totalPages, start + 5);
  const window = Array.from({ length: Math.max(0, end - start) }, (_, i) => start + i);

  return (
    <div className="mt-3.5 flex min-h-8 flex-wrap items-center justify-between gap-3">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-1.5">
        <span className="text-sm text-ink-500">
          {t("showing", { from, to, total: totalElements })}
        </span>
        {chooser}
      </div>

      <div className={`flex items-center gap-1.5 ${totalPages <= 1 ? "hidden" : ""}`}>
        <Button
          size="sm"
          variant="light"
          disabled={page === 0}
          onClick={() => onPage(page - 1)}
        >
          ‹ {t("prev")}
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
          {t("next")} ›
        </Button>
      </div>
    </div>
  );
}
