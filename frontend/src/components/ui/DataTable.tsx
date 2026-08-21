"use client";

import { useTranslations } from "next-intl";
import type { ReactNode } from "react";

export interface Column<T> {
  /** Stable identifier; also the React key for the cell. */
  key: string;
  header: ReactNode;
  render: (row: T, index: number) => ReactNode;
  /** Right-aligns and uses the tabular font — for money and counts. */
  numeric?: boolean;
  align?: "left" | "center" | "right";
  width?: string;
  /** Hidden below the `sm` breakpoint, for columns that are nice-to-have. */
  hideOnMobile?: boolean;
}

export function DataTable<T>({
  columns,
  rows,
  rowKey,
  loading = false,
  emptyMessage,
  onRowClick,
  height = "var(--table-scroll-height)",
  fill = false,
}: {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T, index: number) => string | number;
  loading?: boolean;
  /** Defaults to the translated "no data" string. */
  emptyMessage?: ReactNode;
  onRowClick?: (row: T) => void;
  /**
   * A fixed height for the scrolling body, not a maximum.
   *
   * <p>Fixed on purpose: with a maximum, the box is as tall as its contents
   * until it hits the cap, so the same screen is one height with three rows,
   * another with twelve, and a third while loading. Everything below it — the
   * pager, the buttons — moves as data arrives. A fixed height costs some empty
   * space on a short list and buys a layout that never jumps.
   *
   * <p>Pass "none" for a table that must never clip, such as a receipt being
   * printed. Ignored when `fill` is set.
   */
  height?: string;
  /**
   * Grow to fill the remaining height of a flex-column parent, instead of
   * taking a fixed height. Use inside {@link ListPage}, which is what gives
   * the column a definite height to divide up.
   */
  fill?: boolean;
}) {
  const t = useTranslations("common");
  function alignment(c: Column<T>) {
    if (c.numeric || c.align === "right") return "text-right";
    if (c.align === "center") return "text-center";
    return "text-left";
  }

  return (
    // The wrapper is the scroller, so the header can stick to its top edge, and
    // the height lives here rather than on the table so the border and rounded
    // corners stay around the whole box, scrollbar included.
    //
    // The floor in the fill case is deliberate. Dividing up the leftover
    // height works until there is barely any: on a phone the stat tiles stack
    // four deep and the toolbar wraps, which left the table 129px — two rows —
    // and would leave nothing at all on a shorter screen. Below the floor the
    // column overflows and the page scrolls, which is the right trade on a
    // phone, where scrolling the page is expected anyway.
    //
    // It is a min-height rather than min-h-0 because a scroll container already
    // has an automatic minimum size of zero, so there is nothing to unset.
    <div
      className={[
        "table-scroll overflow-auto rounded-md border border-ink-200 bg-white",
        fill ? "min-h-55 flex-1" : "",
      ].join(" ")}
      style={fill || height === "none" ? undefined : { height }}
    >
      <table className="w-full border-collapse">
        <thead>
          <tr>
            {columns.map((c) => (
              <th
                key={c.key}
                scope="col"
                style={c.width ? { width: c.width } : undefined}
                className={[
                  "sticky top-0 z-10 whitespace-nowrap bg-ink-100 px-3 py-2.5",
                  "text-xs font-semibold uppercase tracking-wide text-ink-500",
                  // An inset shadow, not border-b: on a collapsed-border table
                  // the bottom border belongs to the row boundary and scrolls
                  // away with the rows, leaving the header floating.
                  "shadow-[inset_0_-1px_0_var(--color-ink-300)]",
                  alignment(c),
                  c.hideOnMobile ? "hidden sm:table-cell" : "",
                ].join(" ")}
              >
                {c.header}
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {loading && (
            <tr>
              <td colSpan={columns.length} className="px-3 py-10 text-center text-sm text-ink-500">
                <span className="inline-flex items-center gap-2">
                  <span className="h-3.5 w-3.5 animate-spin rounded-full border-2 border-teal-600 border-t-transparent" />
                  {t("loading")}
                </span>
              </td>
            </tr>
          )}

          {!loading && rows.length === 0 && (
            <tr>
              <td colSpan={columns.length} className="px-3 py-10 text-center text-sm text-ink-500">
                {emptyMessage ?? t("noData")}
              </td>
            </tr>
          )}

          {!loading &&
            rows.map((row, i) => (
              <tr
                key={rowKey(row, i)}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
                className={`border-b border-ink-200 last:border-0 hover:bg-ink-50 ${
                  onRowClick ? "cursor-pointer" : ""
                }`}
              >
                {columns.map((c) => (
                  <td
                    key={c.key}
                    className={[
                      "px-3 py-2.5 text-sm",
                      alignment(c),
                      c.numeric ? "font-[family-name:var(--font-num)]" : "",
                      c.hideOnMobile ? "hidden sm:table-cell" : "",
                    ].join(" ")}
                  >
                    {c.render(row, i)}
                  </td>
                ))}
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
}
