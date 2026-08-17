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
}: {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T, index: number) => string | number;
  loading?: boolean;
  /** Defaults to the translated "no data" string. */
  emptyMessage?: ReactNode;
  onRowClick?: (row: T) => void;
}) {
  const t = useTranslations("common");
  function alignment(c: Column<T>) {
    if (c.numeric || c.align === "right") return "text-right";
    if (c.align === "center") return "text-center";
    return "text-left";
  }

  return (
    <div className="overflow-x-auto rounded-md border border-ink-200 bg-white">
      <table className="w-full border-collapse">
        <thead>
          <tr>
            {columns.map((c) => (
              <th
                key={c.key}
                scope="col"
                style={c.width ? { width: c.width } : undefined}
                className={[
                  "whitespace-nowrap border-b border-ink-300 bg-ink-100 px-3 py-2.5",
                  "text-xs font-semibold uppercase tracking-wide text-ink-500",
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
