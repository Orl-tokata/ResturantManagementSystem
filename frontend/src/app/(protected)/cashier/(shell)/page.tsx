"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import {
  Alert,
  Badge,
  Button,
  Card,
  DataTable,
  StatGrid,
  StatTile,
  toneForOrderStatus,
  type Column,
} from "@/components/ui";
import { get, type PageResponse } from "@/lib/api";
import { errorMessage } from "@/lib/errors";
import { formatUsd } from "@/lib/format";
import { ORDER_STATUS_LABEL, type Order } from "@/types/order";
import type { CashierSummary } from "@/types/report";

export default function CashierHomePage() {
  const summary = useQuery({
    queryKey: ["dashboard", "cashier"],
    queryFn: () => get<CashierSummary>("/dashboard/cashier"),
  });

  const recent = useQuery({
    queryKey: ["orders", { size: 5, page: 0 }],
    queryFn: () => get<PageResponse<Order>>("/orders", { size: 5, page: 0 }),
  });

  const s = summary.data;

  const columns: Column<Order>[] = [
    { key: "inv", header: "វិក្កយបត្រ", render: (r) => r.invoiceNo },
    { key: "table", header: "តុ", render: (r) => r.tableName ?? "—" },
    { key: "items", header: "មុខម្ហូប", numeric: true, render: (r) => r.items.length },
    { key: "total", header: "សរុប", numeric: true, render: (r) => formatUsd(r.total) },
    {
      key: "status",
      header: "ស្ថានភាព",
      render: (r) => (
        <Badge tone={toneForOrderStatus(r.status)}>{ORDER_STATUS_LABEL[r.status]}</Badge>
      ),
    },
    {
      key: "go",
      header: "",
      align: "right",
      render: (r) =>
        r.status === "OPEN" ? (
          <Link href={`/cashier/order?tableId=${r.tableId ?? ""}`}>
            <Button size="sm" variant="accent">
              បន្ត
            </Button>
          </Link>
        ) : (
          <Link href={`/cashier/receipt/${r.id}`}>
            <Button size="sm" variant="ghost">
              🧾
            </Button>
          </Link>
        ),
    },
  ];

  return (
    <>
      {summary.isError && <Alert tone="error">{errorMessage(summary.error)}</Alert>}

      <StatGrid>
        <StatTile tone={1} label="ការលក់ថ្ងៃនេះ · Today's sales" value={formatUsd(s?.todaySales ?? 0)} />
        <StatTile tone={3} label="វិក្កយបត្រថ្ងៃនេះ · Invoices" value={s?.todayInvoices ?? "—"} />
        <StatTile
          tone={2}
          label="តុកំពុងប្រើ · Occupied"
          value={s ? `${s.tablesOccupied} / ${s.tablesTotal}` : "—"}
        />
        <StatTile tone={4} label="វិក្កយបត្រកំពុងបើក · Open bills" value={s?.openBills ?? "—"} />
      </StatGrid>

      <h2 className="mb-3 text-xl font-semibold">ចាប់ផ្តើមរហ័ស · Quick actions</h2>

      <div className="mb-4 flex flex-wrap gap-2">
        <Link href="/cashier/tables">
          <Button variant="accent" size="lg">
            🪑 ជ្រើសរើសតុ · Select table
          </Button>
        </Link>
        <Link href="/cashier/order">
          <Button variant="primary" size="lg">
            🛒 បញ្ជាទិញថ្មី · New order
          </Button>
        </Link>
        <Link href="/cashier/history">
          <Button variant="ghost" size="lg">
            🕘 ប្រវត្តិ · History
          </Button>
        </Link>
      </div>

      <Card
        title="បញ្ជាទិញថ្មីៗ · Recent orders"
        action={
          <Link href="/cashier/history">
            <Button size="sm" variant="ghost">
              មើលទាំងអស់ · View all
            </Button>
          </Link>
        }
        padded={false}
      >
        <DataTable
          columns={columns}
          rows={recent.data?.content ?? []}
          rowKey={(r) => r.id}
          loading={recent.isLoading}
          emptyMessage="មិនទាន់មានបញ្ជាទិញ · No orders yet"
        />
      </Card>
    </>
  );
}
