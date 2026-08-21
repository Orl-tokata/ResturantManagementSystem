"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { BarChart } from "@/components/charts/BarChart";
import {
  Alert,
  Badge,
  Button,
  Card,
  DataTable,
  StatGrid,
  StatTile,
  type Column,
} from "@/components/ui";
import { get } from "@/lib/api";
import { useApiError } from "@/lib/use-api-error";
import { formatUsd } from "@/lib/format";
import {
  dayOfMonth,
  weekdayIndex,
  type BestSeller,
  type DashboardSummary,
  type LowStockRow,
} from "@/types/report";

export default function AdminDashboardPage() {
  const t = useTranslations("dashboard");
  const tc = useTranslations("common");
  const tw = useTranslations("weekday");
  const tNav = useTranslations("nav");
  const tTable = useTranslations("enum.tableStatus");
  const apiError = useApiError();

  const dash = useQuery({
    queryKey: ["dashboard", "summary"],
    queryFn: () => get<DashboardSummary>("/dashboard/summary"),
  });

  const d = dash.data;

  const sellerColumns: Column<BestSeller>[] = [
    {
      key: "rank",
      header: "#",
      width: "44px",
      render: (_r, i) => (
        <span className="grid h-6 w-6 place-items-center rounded-full bg-brand-200 text-xs font-bold text-brand-700">
          {i + 1}
        </span>
      ),
    },
    { key: "name", header: t("dish"), render: (r) => r.productName },
    { key: "qty", header: tc("qty"), numeric: true, render: (r) => r.qty },
    { key: "rev", header: t("revenue"), numeric: true, render: (r) => formatUsd(r.revenue) },
  ];

  const lowColumns: Column<LowStockRow>[] = [
    { key: "name", header: t("item"), render: (r) => r.name },
    {
      key: "qty",
      header: t("remaining"),
      numeric: true,
      render: (r) => (
        <Badge tone={r.qty <= 0 ? "dead" : "warn"}>
          {r.qty} {r.unit}
        </Badge>
      ),
    },
    { key: "min", header: t("minimum"), numeric: true, render: (r) => r.minQty },
  ];

  return (
    <>
      {dash.isError && <Alert tone="error">{apiError(dash.error)}</Alert>}

      <StatGrid>
        <StatTile tone={1} label={t("todaySales")} value={formatUsd(d?.todaySales ?? 0)} />
        <StatTile tone={2} label={t("monthSales")} value={formatUsd(d?.monthSales ?? 0)} />
        <StatTile tone={3} label={t("paidInvoices")} value={d?.paidInvoices ?? "—"} />
        <StatTile tone={4} label={t("lowStock")} value={d?.lowStockCount ?? "—"} />
      </StatGrid>

      <div className="grid gap-4 lg:grid-cols-[1.6fr_1fr] lg:items-start">
        <div className="space-y-4">
          <Card
            title={t("salesLast7")}
            action={
              <Link href="/admin/reports">
                <Button size="sm" variant="ghost">
                  {tNav("reports")}
                </Button>
              </Link>
            }
          >
            {dash.isLoading ? (
              <p className="py-16 text-center text-sm text-ink-500">{tc("loading")}</p>
            ) : (
              <BarChart
                points={(d?.lastSevenDays ?? []).map((p) => ({
                  label: dayOfMonth(p.date),
                  sublabel: tw(String(weekdayIndex(p.date))),
                  value: p.total,
                  detail: `${p.orders} ${t("invoices")}`,
                }))}
                formatValue={formatUsd}
              />
            )}
          </Card>

          <Card title={t("bestSellers")} padded={false}>
            <DataTable
              columns={sellerColumns}
              maxHeight="300px"
              rows={d?.bestSellers ?? []}
              rowKey={(r) => r.productName}
              loading={dash.isLoading}
              emptyMessage={t("noSalesYet")}
            />
          </Card>
        </div>

        <div className="space-y-4">
          <Card title={t("tableStatus")}>
            <div className="space-y-2.5 text-sm">
              <Row label={tTable("FREE")} value={d?.tablesFree} tone="text-success" />
              <Row label={tTable("OCCUPIED")} value={d?.tablesOccupied} tone="text-danger-soft" />
              <Row label={tTable("RESERVED")} value={d?.tablesReserved} tone="text-warning" />
            </div>
            <Link href="/admin/tables">
              <Button variant="ghost" block className="mt-3">
                {t("manageTables")}
              </Button>
            </Link>
          </Card>

          <Card title={t("lowStockAlert")} padded={false}>
            <DataTable
              columns={lowColumns}
              maxHeight="300px"
              rows={d?.lowStock ?? []}
              rowKey={(r) => r.id}
              loading={dash.isLoading}
              emptyMessage={t("stockHealthy")}
            />
            <div className="p-4 pt-3">
              <Link href="/admin/stock">
                <Button variant="admin" block>
                  {t("viewStock")}
                </Button>
              </Link>
            </div>
          </Card>
        </div>
      </div>
    </>
  );
}

function Row({ label, value, tone }: { label: string; value: number | undefined; tone: string }) {
  return (
    <div className="flex items-center justify-between">
      <span>{label}</span>
      <b className={`font-[family-name:var(--font-num)] ${tone}`}>{value ?? "—"}</b>
    </div>
  );
}
