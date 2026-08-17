"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Download } from "lucide-react";
import { BarChart } from "@/components/charts/BarChart";
import {
  Alert,
  Button,
  Card,
  DataTable,
  Input,
  StatGrid,
  StatTile,
  Toolbar,
  type Column,
} from "@/components/ui";
import { api, get } from "@/lib/api";
import { errorMessage } from "@/lib/errors";
import { formatUsd } from "@/lib/format";
import {
  dayOfMonth,
  weekdayKm,
  type BestSeller,
  type CategoryRevenue,
  type SalesReport,
  type SalesRow,
} from "@/types/report";

function iso(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export default function ReportsPage() {
  const today = new Date();
  const monthStart = new Date(today.getFullYear(), today.getMonth(), 1);

  const [from, setFrom] = useState(iso(monthStart));
  const [to, setTo] = useState(iso(today));
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState<string | null>(null);

  const report = useQuery({
    queryKey: ["reports", "sales", { from, to }],
    queryFn: () => get<SalesReport>("/reports/sales", { from, to }),
    placeholderData: (prev) => prev,
  });

  const detail = useQuery({
    queryKey: ["reports", "sales", "detail", { from, to }],
    queryFn: () => get<SalesRow[]>("/reports/sales/detail", { from, to }),
    placeholderData: (prev) => prev,
  });

  const r = report.data;

  /**
   * The CSV endpoint needs the bearer token, so it cannot be a plain <a href>.
   * Fetch it through the same axios instance and hand the blob to the browser.
   */
  async function exportCsv() {
    setExporting(true);
    setExportError(null);
    try {
      const res = await api.get("/reports/sales.csv", {
        params: { from, to },
        responseType: "blob",
      });
      const url = URL.createObjectURL(res.data as Blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `sales-${from}-to-${to}.csv`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (e) {
      setExportError(errorMessage(e, "Could not export the CSV"));
    } finally {
      setExporting(false);
    }
  }

  function setRange(days: number) {
    const start = new Date();
    start.setDate(start.getDate() - days);
    setFrom(iso(start));
    setTo(iso(new Date()));
  }

  function setThisMonth() {
    const now = new Date();
    setFrom(iso(new Date(now.getFullYear(), now.getMonth(), 1)));
    setTo(iso(now));
  }

  const categoryColumns: Column<CategoryRevenue>[] = [
    {
      key: "cat",
      header: "ប្រភេទ · Category",
      render: (c) => (
        <>
          <div className="font-medium">{c.name}</div>
          <div className="text-xs text-ink-500">{c.nameEn}</div>
        </>
      ),
    },
    { key: "qty", header: "ចំនួនលក់", numeric: true, render: (c) => c.qty },
    { key: "rev", header: "ចំណូល", numeric: true, render: (c) => formatUsd(c.revenue) },
    { key: "pct", header: "%", numeric: true, render: (c) => `${c.percent}%` },
  ];

  const sellerColumns: Column<BestSeller>[] = [
    { key: "n", header: "#", width: "44px", render: (_c, i) => i + 1 },
    { key: "name", header: "ម្ហូប · Dish", render: (s) => s.productName },
    { key: "qty", header: "ចំនួន", numeric: true, render: (s) => s.qty },
    { key: "rev", header: "ចំណូល", numeric: true, render: (s) => formatUsd(s.revenue) },
  ];

  const detailColumns: Column<SalesRow>[] = [
    { key: "date", header: "កាលបរិច្ឆេទ", render: (s) => s.date },
    { key: "inv", header: "វិក្កយបត្រ", render: (s) => s.invoiceNo },
    { key: "table", header: "តុ", hideOnMobile: true, render: (s) => s.tableName || "—" },
    { key: "cashier", header: "អ្នកគិតលុយ", hideOnMobile: true, render: (s) => s.cashierName || "—" },
    { key: "items", header: "មុខម្ហូប", numeric: true, render: (s) => s.itemCount },
    { key: "total", header: "សរុប", numeric: true, render: (s) => formatUsd(s.total) },
    { key: "cost", header: "ថ្លៃដើម", numeric: true, hideOnMobile: true, render: (s) => formatUsd(s.cost) },
    {
      key: "profit",
      header: "ចំណេញ",
      numeric: true,
      render: (s) => <span className="font-semibold text-success">{formatUsd(s.profit)}</span>,
    },
    { key: "pay", header: "វិធីបង់", hideOnMobile: true, render: (s) => s.paymentMethod || "—" },
  ];

  return (
    <>
      {report.isError && <Alert tone="error">{errorMessage(report.error)}</Alert>}
      {exportError && <Alert tone="error">{exportError}</Alert>}

      <Toolbar
        left={
          <>
            <Input
              type="date"
              value={from}
              onChange={(e) => setFrom(e.target.value)}
              className="w-auto"
              aria-label="From date"
            />
            <span className="text-ink-500">→</span>
            <Input
              type="date"
              value={to}
              onChange={(e) => setTo(e.target.value)}
              className="w-auto"
              aria-label="To date"
            />
            <Button size="sm" variant="light" onClick={() => setRange(6)}>
              ៧ ថ្ងៃ
            </Button>
            <Button size="sm" variant="light" onClick={() => setRange(29)}>
              ៣០ ថ្ងៃ
            </Button>
            <Button size="sm" variant="light" onClick={setThisMonth}>
              ខែនេះ · This month
            </Button>
          </>
        }
        right={
          <>
            <Button variant="ghost" onClick={exportCsv} loading={exporting}>
              <Download size={15} /> CSV
            </Button>
            <Button variant="ghost" onClick={() => window.print()}>
              🖨️ បោះពុម្ព
            </Button>
          </>
        }
      />

      <StatGrid>
        <StatTile tone={1} label="ចំណូលសរុប · Revenue" value={formatUsd(r?.revenue ?? 0)} />
        <StatTile tone={2} label="ថ្លៃដើម · Cost" value={formatUsd(r?.cost ?? 0)} />
        <StatTile tone={3} label="ចំណេញដុល · Gross profit" value={formatUsd(r?.grossProfit ?? 0)} />
        <StatTile tone={4} label="វិក្កយបត្រ · Invoices" value={r?.invoiceCount ?? "—"} />
      </StatGrid>

      <Card title="ចំណូលតាមថ្ងៃ · Revenue by day" className="mb-4">
        {report.isLoading ? (
          <p className="py-16 text-center text-sm text-ink-500">កំពុងផ្ទុក…</p>
        ) : (
          <>
            <BarChart
              points={(r?.daily ?? []).map((p) => ({
                label: dayOfMonth(p.date),
                sublabel: weekdayKm(p.date),
                value: p.total,
                detail: `${p.orders} វិក្កយបត្រ · invoices`,
              }))}
              formatValue={formatUsd}
              height={220}
            />
            <p className="mt-3 text-xs text-ink-500">
              មធ្យមក្នុងវិក្កយបត្រ · Average sale{" "}
              <b className="text-ink-900">{formatUsd(r?.averageSale ?? 0)}</b> · រឹមចំណេញ · margin{" "}
              <b className="text-ink-900">{r?.marginPercent ?? 0}%</b>
            </p>
          </>
        )}
      </Card>

      <div className="mb-4 grid gap-4 lg:grid-cols-2 lg:items-start">
        <Card title="ចំណូលតាមប្រភេទ · Revenue by category" padded={false}>
          <DataTable
            columns={categoryColumns}
            rows={r?.byCategory ?? []}
            rowKey={(c) => c.name}
            loading={report.isLoading}
            emptyMessage="គ្មានការលក់ក្នុងចន្លោះពេលនេះ · No sales in this range"
          />
          <p className="px-4 py-3 text-xs text-ink-500">
            Line totals before VAT, and only for dishes still in the catalog — a deleted
            product keeps its receipt but drops out of this breakdown.
          </p>
        </Card>

        <Card title="ម្ហូបលក់ដាច់ · Best sellers" padded={false}>
          <DataTable
            columns={sellerColumns}
            rows={r?.bestSellers ?? []}
            rowKey={(s) => s.productName}
            loading={report.isLoading}
            emptyMessage="គ្មានការលក់ · No sales in this range"
          />
        </Card>
      </div>

      <Card
        title="លម្អិតការលក់ · Sales detail"
        action={
          <span className="text-xs text-ink-500">{detail.data?.length ?? 0} វិក្កយបត្រ</span>
        }
        padded={false}
      >
        <DataTable
          columns={detailColumns}
          rows={detail.data ?? []}
          rowKey={(s) => s.invoiceNo}
          loading={detail.isLoading}
          emptyMessage="គ្មានវិក្កយបត្រ · No paid invoices in this range"
        />
      </Card>
    </>
  );
}
