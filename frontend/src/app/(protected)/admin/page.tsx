"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
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
import { errorMessage } from "@/lib/errors";
import { formatUsd } from "@/lib/format";
import {
  dayOfMonth,
  weekdayKm,
  type BestSeller,
  type DashboardSummary,
  type LowStockRow,
} from "@/types/report";

export default function AdminDashboardPage() {
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
    { key: "name", header: "ម្ហូប · Dish", render: (r) => r.productName },
    { key: "qty", header: "ចំនួន", numeric: true, render: (r) => r.qty },
    { key: "rev", header: "ចំណូល", numeric: true, render: (r) => formatUsd(r.revenue) },
  ];

  const lowColumns: Column<LowStockRow>[] = [
    { key: "name", header: "ទំនិញ · Item", render: (r) => r.name },
    {
      key: "qty",
      header: "នៅសល់",
      numeric: true,
      render: (r) => (
        <Badge tone={r.qty <= 0 ? "dead" : "warn"}>
          {r.qty} {r.unit}
        </Badge>
      ),
    },
    { key: "min", header: "អប្បបរមា", numeric: true, render: (r) => r.minQty },
  ];

  return (
    <>
      {dash.isError && <Alert tone="error">{errorMessage(dash.error)}</Alert>}

      <StatGrid>
        <StatTile tone={1} label="ការលក់ថ្ងៃនេះ · Today" value={formatUsd(d?.todaySales ?? 0)} />
        <StatTile tone={2} label="ការលក់ខែនេះ · This month" value={formatUsd(d?.monthSales ?? 0)} />
        <StatTile tone={3} label="វិក្កយបត្របានបង់ · Paid invoices" value={d?.paidInvoices ?? "—"} />
        <StatTile tone={4} label="ស្តុកជិតអស់ · Low stock" value={d?.lowStockCount ?? "—"} />
      </StatGrid>

      <div className="grid gap-4 lg:grid-cols-[1.6fr_1fr] lg:items-start">
        <div className="space-y-4">
          <Card
            title="ការលក់ ៧ ថ្ងៃចុងក្រោយ · Sales, last 7 days"
            action={
              <Link href="/admin/reports">
                <Button size="sm" variant="ghost">
                  របាយការណ៍ · Reports
                </Button>
              </Link>
            }
          >
            {dash.isLoading ? (
              <p className="py-16 text-center text-sm text-ink-500">កំពុងផ្ទុក…</p>
            ) : (
              <BarChart
                points={(d?.lastSevenDays ?? []).map((p) => ({
                  label: dayOfMonth(p.date),
                  sublabel: weekdayKm(p.date),
                  value: p.total,
                  detail: `${p.orders} វិក្កយបត្រ · invoices`,
                }))}
                formatValue={formatUsd}
              />
            )}
          </Card>

          <Card title="ម្ហូបលក់ដាច់ខែនេះ · Best sellers this month" padded={false}>
            <DataTable
              columns={sellerColumns}
              rows={d?.bestSellers ?? []}
              rowKey={(r) => r.productName}
              loading={dash.isLoading}
              emptyMessage="មិនទាន់មានការលក់ · No sales yet this month"
            />
          </Card>
        </div>

        <div className="space-y-4">
          <Card title="ស្ថានភាពតុ · Table status">
            <div className="space-y-2.5 text-sm">
              <Row label="ទំនេរ · Free" value={d?.tablesFree} tone="text-success" />
              <Row label="កំពុងប្រើ · Occupied" value={d?.tablesOccupied} tone="text-danger-soft" />
              <Row label="កក់ទុក · Reserved" value={d?.tablesReserved} tone="text-warning" />
            </div>
            <Link href="/admin/tables">
              <Button variant="ghost" block className="mt-3">
                គ្រប់គ្រងតុ · Manage tables
              </Button>
            </Link>
          </Card>

          <Card title="ស្តុកជិតអស់ · Low stock alert" padded={false}>
            <DataTable
              columns={lowColumns}
              rows={d?.lowStock ?? []}
              rowKey={(r) => r.id}
              loading={dash.isLoading}
              emptyMessage="ស្តុកគ្រប់គ្រាន់ · Everything is above its reorder level"
            />
            <div className="p-4 pt-3">
              <Link href="/admin/stock">
                <Button variant="admin" block>
                  មើលស្តុក · View stock
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
